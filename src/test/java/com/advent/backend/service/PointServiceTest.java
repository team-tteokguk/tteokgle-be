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

    @MockitoBean private NotificationRepository notificationRepository;

    private Member buyer;
    private Member seller;
    private Store store;
    private Item item;

    private final Integer BUYER_POINT = 1000;
    private final Integer SELLER_POINT = 0;
    private final Integer ITEM_COST = 100;

    @BeforeEach
    void setUp() {
        // 1. 빌더 패턴으로 통환된 엔티티 생성 (일관성 유지)
        buyer =
                Member.builder()
                        .nickname("구매자")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("buyer_id")
                        .point(BUYER_POINT)
                        .build();

        memberRepository.save(buyer);

        seller =
                Member.builder()
                        .nickname("판매자")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("seller_id")
                        .point(SELLER_POINT)
                        .build();

        memberRepository.save(seller);

        Store store = Store.builder().member(seller).title("외요의 상점").build();

        storeRepository.save(store);

        item =
                Item.builder()
                        .store(store)
                        .imageUrl("이미지 url")
                        .cost(ITEM_COST)
                        .contentType(Item.ContentType.NONE)
                        .contentData("{}")
                        .content("내용")
                        .isAvailable(true)
                        .build();

        itemRepository.save(item);
        myTteokRepository.save(MyTteok.builder().member(seller).build());
    }

    @Test
    @DisplayName("송금 성공: 두 멤버 간 포인트 거래 시 잔액이 정확하게 변하는지")
    void should_AccurateChanges_When_PointsTransferredBetweenMembers() {
        pointService.transfer(buyer.getId(), seller.getId(), item.getId());

        Member updatedBuyer = memberRepository.findById(buyer.getId()).orElseThrow();
        Member updatedSeller = memberRepository.findById(seller.getId()).orElseThrow();

        // 3. 최신화된 객체로 검증
        assertThat(updatedBuyer.getPoint()).isEqualTo(BUYER_POINT - ITEM_COST);
        assertThat(updatedSeller.getPoint()).isEqualTo(SELLER_POINT + ITEM_COST);
    }

    @Test
    @DisplayName("잔액 부족 실패: 잔액이 발생할 때 예외가 발생하는지")
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
                                        poorBuyer.getId(), seller.getId(), item.getId()))
                .isInstanceOf(BusinessException.class);

        assertThat(failedBuyer.getPoint()).isEqualTo(50);
    }

    @Test
    @DisplayName("송금 도중 강제로 에러를 발생시켰을 때 잔액이 원래대로 돌아오는지")
    void should_Rollback_when_ErrorOccursDuringTransfer() {
        BDDMockito.doThrow(new RuntimeException("강제 에러 발생"))
                .when(notificationRepository)
                .save(any());

        assertThatThrownBy(() -> pointService.transfer(buyer.getId(), seller.getId(), item.getId()))
                .isInstanceOf(RuntimeException.class);

        Member failedBuyer = memberRepository.findById(buyer.getId()).orElseThrow();

        assertThat(failedBuyer.getPoint()).isEqualTo(BUYER_POINT);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 송금 시 에러가 발생하는지")
    void should_ThrowException_when_TransferToNotFoundedMember() {
        UUID unfoundedMemberId = UUID.randomUUID();

        assertThatThrownBy(
                        () -> pointService.transfer(buyer.getId(), unfoundedMemberId, item.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("N명이 동시에 1명에게 송금할 때 포인트 합계가 정확한지")
    void should_AccuratePoints_when_ManyTransfersToOne() throws InterruptedException {
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
                            pointService.transfer(new_buyer.getId(), seller.getId(), item.getId());
                        } catch (BusinessException e) {
                            System.out.println(e.getMessage());
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
        }

        countDownLatch.await();

        Member resultSeller = memberRepository.findById(seller.getId()).orElseThrow();

        assertThat(resultSeller.getPoint()).isEqualTo(SELLER_POINT + ITEM_COST * threadCount);
    }
}
