package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.*;
import com.advent.backend.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public void transfer(
            Member sender, Member receiver, UUID targetId, Integer cost, String targetName) {
        // 1. 자기 자신 송금 제한
        if (sender.equals(receiver)) {
            throw new BusinessException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED);
        }

        // 5. 송금 금액 유효성 검사
        if (cost <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER_AMOUNT);
        }

        // 6. 비즈니스 로직 수행 (엔티티 내부 검증 로직 실행)
        sender.decreasePoint(cost); // 잔액 부족 시 여기서 예외 발생 -> 자동 롤백
        receiver.addPoint(cost);

        // 8. Point History 저장
        savePointHistory(sender, receiver, targetId, cost, PointHistory.TradeType.USE);
    }

    private void savePointHistory(
            Member sender,
            Member receiver,
            UUID targetId,
            int amount,
            PointHistory.TradeType type) {
        PointHistory history =
                PointHistory.builder()
                        .sender(sender)
                        .receiver(receiver)
                        .amount(amount)
                        .tradeType(type)
                        .targetId(targetId)
                        .build();

        pointHistoryRepository.save(history);
    }
}
