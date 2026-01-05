package com.advent.backend.service.handler;

import com.advent.backend.entity.Notification;
import com.advent.backend.event.TransferEvent;
import com.advent.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventListener {
    private final NotificationRepository notificationRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransferSuccess(TransferEvent event) {
        // 성공 로그 작성
        log.info(
                "[TX_SUCCESS] ID: {}, Sender: {}, Receiver: {}",
                event.txId(),
                event.sender().getId(),
                event.receiver().getId());
        try {
            // 성공 로그 작성
            log.info("[알림 시작]");

            // 알림 발송
            notificationRepository.save(
                    Notification.builder()
                            .member(event.receiver())
                            .notificationType(Notification.NotificationType.SALE)
                            .message(
                                    event.sender().getNickname()
                                            + "님이 "
                                            + event.itemName()
                                            + "을 구매하셨습니다.")
                            .build());
            log.info("[알림 ㄲ<ㅌ]");
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] 알림 저장 실패: {}", e.getMessage());
        }
    }
}
