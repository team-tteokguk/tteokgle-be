package com.advent.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.*;
import com.advent.backend.repository.*;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks private PointService pointService;

    @Mock private PointHistoryRepository pointHistoryRepository;

    private Member memberA; // 구매자
    private Member memberB; // 판매자

    private final Integer A_POINT = 1000;
    private final Integer B_POINT = 1000;
    private final Integer ITEM_COST = 100;

    @Test
    @DisplayName("송금 성공: 두 멤버 간 포인트 거래 시 잔액이 정확하게 변한다.")
    void should_AccurateChanges_When_PointsTransferredBetweenMembers() {
        memberA =
                Member.builder()
                        .nickname("구매자")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("buyer_id")
                        .point(A_POINT)
                        .build();

        memberB =
                Member.builder()
                        .nickname("판매자")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("seller_id")
                        .point(B_POINT)
                        .build();

        pointService.transfer(memberA, memberB, UUID.randomUUID(), ITEM_COST, "외요의 고명");

        assertThat(memberA.getPoint()).isEqualTo(A_POINT - ITEM_COST);
        assertThat(memberB.getPoint()).isEqualTo(B_POINT + ITEM_COST);

        verify(pointHistoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("잔액 부족 실패: 잔액이 발생할 때 예외가 발생한다.")
    void should_ThrowException_when_BuyerHasInUnSufficentPoints() {
        memberA =
                Member.builder()
                        .nickname("거지 구매자")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("buyer_id")
                        .point(50)
                        .build();

        memberB =
                Member.builder()
                        .nickname("판매자")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("seller_id")
                        .point(B_POINT)
                        .build();

        assertThatThrownBy(
                        () ->
                                pointService.transfer(
                                        memberA, memberB, UUID.randomUUID(), ITEM_COST, "외요의 고명"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_BALANCE.getMessage());
    }
}
