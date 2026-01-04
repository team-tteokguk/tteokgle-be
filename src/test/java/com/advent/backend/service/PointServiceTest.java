package com.advent.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.*;
import com.advent.backend.repository.*;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class PointServiceTest {

    @Autowired private PointService pointService;
    @Autowired private MemberRepository memberRepository; // 리포지토리 주입
    @Autowired private StoreRepository storeRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private MyTteokRepository myTteokRepository;
    @Autowired private MyItemRepository myItemRepository;

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
                        .imageUrl("이미지 url")
                        .cost(ITEM_COSTB)
                        .contentType(Item.ContentType.NONE)
                        .contentData("{}")
                        .content("내용")
                        .isAvailable(true)
                        .build();

        itemRepository.save(itemB);
        myTteokRepository.save(MyTteok.builder().member(memberB).build());
    }

    @Test
    @DisplayName("송금 성공: 두 멤버 간 포인트 거래 시 잔액이 정확하게 변한다.")
    void should_AccurateChanges_When_PointsTransferredBetweenMembers() {
        pointService.transfer(memberA.getId(), memberB.getId(), itemB.getId());

        Member updatedBuyer = memberRepository.findById(memberA.getId()).orElseThrow();
        Member updatedSeller = memberRepository.findById(memberB.getId()).orElseThrow();

        // 3. 최신화된 객체로 검증
        assertThat(updatedBuyer.getPoint()).isEqualTo(A_POINT - ITEM_COSTB);
        assertThat(updatedSeller.getPoint()).isEqualTo(B_POINT + ITEM_COSTB);
        //        System.out.println(myItemRepository.findByMemberId(updatedBuyer.getId()));
        assertThat(myItemRepository.findByMemberId(updatedBuyer.getId()).get(0).getItem().getId())
                .isEqualTo(itemB.getId());
    }

    @Test
    @DisplayName("잔액 부족 실패: 잔액이 발생할 때 예외가 발생한다.")
    void should_ThrowException_when_BuyerHasInSufficentPoints() {
        Member poorBuyer =
                Member.builder()
                        .nickname("거지")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("거지의 아이디")
                        .point(50)
                        .build();

        memberRepository.save(poorBuyer);
        Member failedBuyer = memberRepository.findById(poorBuyer.getId()).orElseThrow();

        assertThatThrownBy(
                        () ->
                                pointService.transfer(
                                        poorBuyer.getId(), memberB.getId(), itemB.getId()))
                .isInstanceOf(BusinessException.class);

        assertThat(failedBuyer.getPoint()).isEqualTo(50);
    }

    @Test
    @DisplayName("롤백 테스트: 송금 도중 강제로 에러를 발생시켰을 때 잔액이 원래대로 돌아온다.")
    void should_Rollback_when_ErrorOccursDuringTransfer() {
        BDDMockito.doThrow(new RuntimeException("강제 에러 발생"))
                .when(notificationRepository)
                .save(any());

        assertThatThrownBy(
                        () ->
                                pointService.transfer(
                                        memberA.getId(), memberB.getId(), itemB.getId()))
                .isInstanceOf(RuntimeException.class);

        Member failedBuyer = memberRepository.findById(memberA.getId()).orElseThrow();

        assertThat(failedBuyer.getPoint()).isEqualTo(A_POINT);
    }

    @Test
    @DisplayName("유효성 테스트: 존재하지 않는 ID로 송금 시 에러가 발생한다.")
    void should_ThrowException_when_TransferToNotFoundedMember() {
        UUID unfoundedMemberId = UUID.randomUUID();

        assertThatThrownBy(
                        () ->
                                pointService.transfer(
                                        memberA.getId(), unfoundedMemberId, itemB.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("다 대 일 송금: N명이 동시에 한 명에게 송금할 때 포인트 합계가 정확히 반영된다.")
    void should_AccuratePoints_when_MultipleMembersTransferToOne() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            Member new_buyer =
                    Member.builder()
                            .nickname("군중" + i)
                            .socialType(Member.SocialType.KAKAO)
                            .socialId("군중의 아이디" + i)
                            .point(100)
                            .build();
            memberRepository.save(new_buyer);

            executorService.submit(
                    () -> {
                        try {
                            pointService.transfer(
                                    new_buyer.getId(), memberB.getId(), itemB.getId());
                        } catch (BusinessException e) {
                            System.out.println(e.getMessage());
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
        }

        countDownLatch.await();

        Member resultSeller = memberRepository.findById(memberB.getId()).orElseThrow();

        assertThat(resultSeller.getPoint()).isEqualTo(B_POINT + ITEM_COSTB * threadCount);
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
                        pointService.transfer(memberA.getId(), memberB.getId(), itemB.getId());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        // 2. 멤버 B -> 상점 A에서 구매
        executorService.submit(
                () -> {
                    try {
                        pointService.transfer(memberB.getId(), memberA.getId(), itemA.getId());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        countDownLatch.await();

        Member resultA = memberRepository.findById(memberA.getId()).orElseThrow();
        Member resultB = memberRepository.findById(memberB.getId()).orElseThrow();

        assertThat(resultA.getPoint()).isEqualTo(A_POINT - ITEM_COSTB + ITEM_COSTA);
        assertThat(resultB.getPoint()).isEqualTo(B_POINT - ITEM_COSTA + ITEM_COSTB);
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

        executorService.submit(
                () -> {
                    try {
                        pointService.transfer(memberA.getId(), memberB.getId(), itemB.getId());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        executorService.submit(
                () -> {
                    try {
                        pointService.transfer(buyer.getId(), memberA.getId(), itemA.getId());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        countDownLatch.await();

        Member resultA = memberRepository.findById(memberA.getId()).orElseThrow();

        assertThat(resultA.getPoint()).isEqualTo(A_POINT - ITEM_COSTB + ITEM_COSTA);
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
                        pointService.transfer(memberB.getId(), buyer.getId(), buyerItem.getId());
                    } finally {
                        countDownLatch.countDown();
                    }
                });

        // buyer -> A 구매
        executorService.submit(
                () -> {
                    try {
                        pointService.transfer(buyer.getId(), memberA.getId(), itemA.getId());
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
}
