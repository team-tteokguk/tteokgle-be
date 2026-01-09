package com.advent.backend.listener;

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

    // [수정] final 키워드 필수! (그래야 의존성 주입이 됨)
    private final NotificationService notificationService;

    /** 송금(구매) 알림 PurchaseEvent(Member sender, Member receiver, String itemName, ...) */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePurchaseEvent(PurchaseEvent event) {
        log.info(
                "[알림] 구매 발생: {} -> {} (상품: {})",
                event.sender().getNickname(),
                event.receiver().getNickname(),
                event.itemName());

        // 서비스의 전용 메서드 호출
        notificationService.sendSaleNotification(
                event.receiver(), // 판매자 (알림 받을 사람)
                event.sender().getNickname(), // 구매자 닉네임
                event.itemName() // 상품명
                );
    }

    /** 방명록 댓글 알림 CommentEvent(Member commenter, Member owner, GuestBook guestBook) */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentEvent(CommentEvent event) {
        log.info(
                "[알림] 댓글 발생: {} -> {}",
                event.commenter().getNickname(),
                event.owner().getNickname());

        // 방명록이 달린 상점 ID 추출
        // (GuestBook 엔티티를 통해 Store ID를 가져옵니다)
        notificationService.sendCommentNotification(
                event.owner(), // 상점 주인 (알림 받을 사람)
                event.commenter().getNickname(), // 작성자 닉네임
                event.guestBook().getStore().getId() // 상점 ID (링크 생성용)
                );
    }

    /** 구독 알림 SubscribeEvent(Member subscriber, Member target) */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscribeEvent(SubscribeEvent event) {
        log.info(
                "[알림] 구독 발생: {} -> {}",
                event.subscriber().getNickname(),
                event.target().getNickname());

        notificationService.sendSubscribeNotification(
                event.target(), // 구독 당한 사람 (알림 받을 사람)
                event.subscriber().getNickname() // 구독한 사람 닉네임
                );
    }
}
