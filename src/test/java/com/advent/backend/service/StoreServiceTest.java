package com.advent.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.entity.*;
import com.advent.backend.repository.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
public class StoreServiceTest {
    @Autowired private MemberService memberService;
    @Autowired private StoreService storeService;
    @Autowired private MemberRepository memberRepository; // 리포지토리 주입
    @Autowired private StoreRepository storeRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private MyTteokRepository myTteokRepository;
    @MockitoSpyBean private MyItemRepository myItemRepository;
    @Autowired private PointHistoryRepository pointHistoryRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    @MockitoBean private NotificationRepository notificationRepository;

    private Member memberA; // 구매자
    private Member memberB; // 판매자
    private Store storeA;
    private Store storeB;
    private Item itemA;
    private Item itemB;

    private final Integer A_POINT = 1000;
    private final Integer B_POINT = 1000;
    private final Integer ITEM_COSTA = 100;
    private final Integer ITEM_COSTB = 100;

    @BeforeEach
    void setUp() {
        // 1. 빌더 패턴으로 통환된 엔티티 생성 (일관성 유지)
        memberA =
                Member.builder()
                        .nickname("구매자")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("buyer_id")
                        .point(A_POINT)
                        .build();

        memberRepository.save(memberA);

        storeA = Store.builder().member(memberA).title("외요의 상점").build();

        storeRepository.save(storeA);

        itemA =
                Item.builder()
                        .store(storeA)
                        .name("이름")
                        .imageUrl("이미지 url")
                        .cost(ITEM_COSTA)
                        .contentType(Item.ContentType.NONE)
                        .contentData("{}")
                        .content("내용")
                        .isAvailable(true)
                        .build();

        itemRepository.save(itemA);
        myTteokRepository.save(MyTteok.builder().member(memberA).build());

        memberB =
                Member.builder()
                        .nickname("판매자")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("seller_id")
                        .point(B_POINT)
                        .build();

        memberRepository.save(memberB);

        storeB = Store.builder().member(memberB).title("외요의 상점").build();

        storeRepository.save(storeB);

        itemB =
                Item.builder()
                        .store(storeB)
                        .name("이름")
                        .imageUrl("이미지 url")
                        .cost(ITEM_COSTB)
                        .contentType(Item.ContentType.NONE)
                        .contentData("{}")
                        .content("내용")
                        .isAvailable(true)
                        .build();

        itemRepository.save(itemB);
        myTteokRepository.save(MyTteok.builder().member(memberB).build());

        Notification notification =
                Notification.builder()
                        .member(memberA)
                        .notificationType(Notification.NotificationType.COMMENT)
                        .message("네 고명에 큰 인상을 받았어")
                        .build();

        notificationRepository.saveAndFlush(notification);
    }

    @Test
    @DisplayName("구매 성공.")
    void should_AccurateChanges_When_PointsTransferredBetweenMembers() {
        // 멤버 A가 멤버 B의 상품을 구매
        storeService.purchaseItem(memberA.getId(), itemB.getId());

        // 구매가 성공적으로 이루어졌는지 확인
        Member updatedBuyer = memberRepository.findById(memberA.getId()).orElseThrow();
        Member updatedSeller = memberRepository.findById(memberB.getId()).orElseThrow();

        // 1. 돈이 ITEM_COSTB 만큼 깎이고 추가됐는가?
        verifyPointBalances(memberA.getId(), A_POINT - ITEM_COSTB);
        verifyPointBalances(memberB.getId(), B_POINT + ITEM_COSTB);

        // 2. A의 '나의 떡국'에 ItemB가 저장되었는가?
        verifyItemDelivery(memberA.getId(), itemB.getId());

        // 3. 포인트 히스토리에 관련 기록이 있는가?
        verifyPointHistory(memberA.getId(), memberB.getId(), ITEM_COSTB, itemB.getId());

        // 4. 알림이 잘 발송되었는가? (차후 추가)
    }

    @Test
    @DisplayName("롤백 테스트: 송금 도중 강제로 에러를 발생시켰을 때 잔액이 원래대로 돌아온다.")
    void should_Rollback_when_ErrorOccursDuringTransfer() {
        BDDMockito.doThrow(new RuntimeException("강제 에러 발생")).when(myItemRepository).save(any());

        assertThatThrownBy(() -> storeService.purchaseItem(memberA.getId(), itemB.getId()))
                .isInstanceOf(RuntimeException.class);

        Member failedBuyer = memberRepository.findById(memberA.getId()).orElseThrow();

        assertThat(failedBuyer.getPoint()).isEqualTo(A_POINT);
    }

    @Test
    @DisplayName("유효성 테스트: 존재하지 않는 ID로 송금 시 에러가 발생한다.")
    void should_ThrowException_when_TransferToNotFoundedMember() {
        UUID unfoundedMemberId = UUID.randomUUID();

        assertThatThrownBy(() -> storeService.purchaseItem(unfoundedMemberId, itemB.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("다 대 일 송금: N명이 동시에 한 명에게 송금할 때 포인트 합계가 정확히 반영된다.")
    void should_AccuratePoints_when_MultipleMembersTransferToOne() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            Member new_buyer =
                    Member.builder()
                            .nickname("군중" + i)
                            .socialType(Member.SocialType.KAKAO)
                            .socialId("군중의 아이디" + i)
                            .point(1000)
                            .build();
            memberRepository.save(new_buyer);

            myTteokRepository.save(MyTteok.builder().member(new_buyer).build());

            executorService.submit(
                    () -> {
                        try {
                            storeService.purchaseItem(new_buyer.getId(), itemB.getId());
                        } catch (BusinessException e) {
                            System.out.println(e.getMessage());
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
        }

        countDownLatch.await();

        verifyPointBalances(memberB.getId(), B_POINT + ITEM_COSTB * threadCount);
    }

    @Test
    @DisplayName("데드락 테스트: 멤버 A와 멤버 B가 서로의 상점에서 물건을 구매했을 때, 데드락 없이 잔액이 정확히 반영된다.")
    void should_MaintainConsistency_when_MutualTransferOccurs() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch countDownLatch = new CountDownLatch(2);

        // 1. 멤버 A -> 상점 B에서 구매
        executorService.submit(
                () -> {
                    try {
                        storeService.purchaseItem(memberA.getId(), itemB.getId());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        // 2. 멤버 B -> 상점 A에서 구매
        executorService.submit(
                () -> {
                    try {
                        storeService.purchaseItem(memberB.getId(), itemA.getId());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        countDownLatch.await();

        verifyPointBalances(memberA.getId(), A_POINT - ITEM_COSTB + ITEM_COSTA);
        verifyPointBalances(memberB.getId(), B_POINT - ITEM_COSTA + ITEM_COSTB);

        verifyItemDelivery(memberA.getId(), itemB.getId());
        verifyItemDelivery(memberB.getId(), itemA.getId());

        verifyPointHistory(memberA.getId(), memberB.getId(), ITEM_COSTB, itemB.getId());
        verifyPointHistory(memberB.getId(), memberA.getId(), ITEM_COSTB, itemA.getId());
    }

    @Test
    @DisplayName("동시성 테스트: 멤버 A에게 포인트가 입금되는 순간 멤버 A가 물건을 구매했을 때 최종 잔액이 정확히 반영된다.")
    void should_AccuratePoints_when_ConcurrentIncomeAndExpense() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch countDownLatch = new CountDownLatch(2);

        Member buyer =
                Member.builder()
                        .nickname("거지")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("거지의 아이디")
                        .point(100)
                        .build();

        memberRepository.save(buyer);

        myTteokRepository.save(MyTteok.builder().member(buyer).build());

        executorService.submit(
                () -> {
                    try {
                        storeService.purchaseItem(memberA.getId(), itemB.getId());
                    } catch (BusinessException e) {
                        System.out.println(e.getMessage());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        executorService.submit(
                () -> {
                    try {
                        storeService.purchaseItem(buyer.getId(), itemA.getId());
                    } catch (BusinessException e) {
                        System.out.println(e.getMessage());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        countDownLatch.await();

        verifyPointBalances(memberA.getId(), A_POINT - ITEM_COSTB + ITEM_COSTA);

        verifyItemDelivery(memberA.getId(), itemB.getId());
        verifyItemDelivery(buyer.getId(), itemA.getId());

        verifyPointHistory(memberA.getId(), memberB.getId(), ITEM_COSTB, itemB.getId());
        verifyPointHistory(buyer.getId(), memberA.getId(), ITEM_COSTA, itemA.getId());
    }

    @Test
    @DisplayName("순서 보장 테스트: 잔액이 0원인 멤버에게 입금되는 순간 결제 요청이 들어왔을 때 결제가 처리되거나 결제가 실패된다.")
    void should_SucceedOrFail_when_NoPointMemberConcurrentDepositAndWithdraw()
            throws InterruptedException {
        Member buyer =
                Member.builder()
                        .nickname("거지")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("거지의 아이디")
                        .point(0)
                        .build();

        memberRepository.save(buyer);
        Store buyerStore = Store.builder().member(buyer).title("거지의 상점").build();
        storeRepository.save(buyerStore);
        Item buyerItem =
                Item.builder()
                        .name("이름")
                        .store(buyerStore)
                        .imageUrl("이미지 url")
                        .cost(200)
                        .contentType(Item.ContentType.NONE)
                        .contentData("{}")
                        .content("내용")
                        .isAvailable(true)
                        .build();

        itemRepository.save(buyerItem);
        myTteokRepository.save(MyTteok.builder().member(buyer).build());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch countDownLatch = new CountDownLatch(2);

        AtomicBoolean isPurchaseFailed = new AtomicBoolean(false);

        // B -> buyer 구매
        executorService.submit(
                () -> {
                    try {
                        storeService.purchaseItem(memberB.getId(), buyerItem.getId());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        // buyer -> A 구매
        executorService.submit(
                () -> {
                    try {
                        storeService.purchaseItem(buyer.getId(), itemA.getId());
                    } catch (BusinessException e) {
                        isPurchaseFailed.set(true);
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        countDownLatch.await();

        Member resultA = memberRepository.findById(buyer.getId()).orElseThrow();

        if (isPurchaseFailed.get()) {
            System.out.println("실패해서 입금만");
            assertThat(resultA.getPoint()).isEqualTo(200);
        } else {
            System.out.println("성공해서 100");
            assertThat(resultA.getPoint()).isEqualTo(100);
        }
    }

    private void verifyPointBalances(UUID memberId, int expectedPoint) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        assertThat(member.getPoint()).as("잔액 확인").isEqualTo(expectedPoint);
    }

    private void verifyItemDelivery(UUID buyerId, UUID expectedItemId) {
        List<MyItem> myItems = myItemRepository.findByMemberId(buyerId);
        assertThat(myItems).as("구매자 아이템 목록 확인").isNotEmpty();
        assertThat(myItems.get(0).getItem().getId()).isEqualTo(expectedItemId);
    }

    private void verifyPointHistory(UUID senderId, UUID receiverId, int amount, UUID targetId) {
        PointHistory history =
                pointHistoryRepository
                        .findTopBySenderIdOrderByCreatedAtDesc(senderId)
                        .orElseThrow(() -> new AssertionError("히스토리가 생성되지 않았습니다."));

        assertThat(history.getAmount()).isEqualTo(amount);
        assertThat(history.getReceiver().getId()).isEqualTo(receiverId);
        assertThat(history.getTargetId()).isEqualTo(targetId);
        assertThat(history.getTradeType()).isEqualTo(PointHistory.TradeType.USE);
    }

    @Test
    @DisplayName("상점 물건 조회")
    public void should_ReturnListOfItems() {
        // 상점에 물건 등록
        for (int i = 0; i < 9; i++) {
            Item item =
                    Item.builder()
                            .store(storeA)
                            .name("이름")
                            .imageUrl("이미지 url")
                            .cost(ITEM_COSTA)
                            .contentType(Item.ContentType.NONE)
                            .contentData("{}")
                            .content("내용")
                            .isAvailable(true)
                            .build();

            itemRepository.save(item);
        }

        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        Page<ItemDto.StoreItemResponse> result = storeService.getItems(storeA.getId(), pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(5);
        assertThat(result.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품 진열")
    public void should_PublishItem() {
        ItemDto.ItemCreateRequest request =
                ItemDto.ItemCreateRequest.builder()
                        .name("고명")
                        .mediaUrl("{}")
                        .contentType(Item.ContentType.PHOTO)
                        .content("f")
                        .imageUrl("이미지")
                        .build();

        ItemDto.StoreItemResponse response = storeService.publishItem(storeA.getId(), request);

        List<Item> itemList = itemRepository.findAllByStoreId(storeA.getId());

        assertThat(itemList).isNotEmpty();

        boolean exists = itemList.stream().anyMatch(item -> item.getId().equals(response.getId()));
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("상품 진열 삭제")
    public void should_RemoveItem() {
        storeService.removeItem(storeA.getId(), itemA.getId());

        List<Item> itemList = itemRepository.findAllByStoreId(storeA.getId());

        boolean exists = itemList.stream().anyMatch(item -> item.getId().equals(itemA.getId()));

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("상점 즐겨찾기 추가")
    public void should_AddSubscription() {
        storeService.addSubscription(storeA.getId(), memberB.getId());

        Pageable pageable = PageRequest.of(0, 1, Sort.by("createdAt").descending());

        Page<Subscription> result =
                subscriptionRepository.findAllByMemberId(memberB.getId(), pageable);

        Subscription subscription = result.getContent().get(0);

        assertThat(subscription).isNotNull();
        assertThat(subscription.getStore().getId()).isEqualTo(storeA.getId());
        assertThat(subscription.getMember().getId()).isEqualTo(memberB.getId());
    }
}
