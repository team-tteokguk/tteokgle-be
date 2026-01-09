package com.advent.backend.listener;

import com.advent.backend.entity.Notification;
import com.advent.backend.event.CommentEvent;
import com.advent.backend.event.PurchaseEvent;
import com.advent.backend.event.SubscribeEvent;
import com.advent.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private NotificationService notificationService;

    /**
     * 송금 트랜잭션 커밋(성공) 후에만 실행
     *
     * @param event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotification(PurchaseEvent event) {
        log.info(
                "[알림] 송금 트랜잭션 : Sender: {}, Receiver: {}",
                event.sender().getNickname(),
                event.receiver().getNickname());

        String message =
                String.format(
                        "%s님이 %s을(를) 구미했습니다!", event.sender().getNickname(), event.itemName());

        // TODO: 나중에 링크 수정
        String link = "/link";

        notificationService.createNotification(
                event.receiver(), Notification.NotificationType.SALE, message, link);
    }

    /**
     * 방명록 댓글 알림
     *
     * @param event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentEvnet(CommentEvent event) {
        log.info(
                "[알림] 댓글 이벤트 : {} -> {}",
                event.commenter().getNickname(),
                event.owner().getNickname());

        String message =
                String.format(
                        "%s님이 방명록에 글을 남겼어요: \"%s\"",
                        event.commenter().getNickname(), truncate(event.comment(), 10));

        notificationService.createNotification(
                event.owner(),
                Notification.NotificationType.COMMENT,
                message,
                "/my-store/guestbook");
    }

    /**
     * 구독 알림
     *
     * @param event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscribeEvent(SubscribeEvent event) {
        log.info(
                "[알림] 구독 이벤트: {} -> {}",
                event.subscriber().getNickname(),
                event.target().getNickname());

        String message =
                String.format("%s님이 내 상점을 구독하기 시작했습니다.🎉", event.subscriber().getNickname());

        notificationService.createNotification(
                event.target(),
                Notification.NotificationType.SUBSCRIBE,
                message,
                "/my-page/followers");
    }

    /**
     * 문자열 자르기 유틸
     *
     * @param content
     * @param limit
     * @return
     */
    private String truncate(String content, int limit) {
        if (content == null) return "";
        return content.length() > limit ? content.substring(0, limit) + "..." : content;
    }
}
