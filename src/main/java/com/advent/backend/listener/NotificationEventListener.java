package com.advent.backend.listener;

import com.advent.backend.event.CommentEvent;
import com.advent.backend.event.PurchaseEvent;
import com.advent.backend.event.SubscribeEvent;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    /** 송금(구매) 알림 PurchaseEvent(Member sender, Member receiver, String itemName, ...) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

        notificationService.sendPurchaseNotification(
                event.sender(), // 구매자 (알림 받을 사람)
                event.receiver().getNickname(), // 판매자 닉네임
                event.itemName() // 상품명
                );
    }

    /** 방명록 댓글 알림 CommentEvent(String commenterNickname, UUID ownerId, UUID storeId) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentEvent(CommentEvent event) {
        var owner = memberRepository.findById(event.ownerId()).orElse(null);
        if (owner == null) {
            log.warn("[알림] 댓글 알림 스킵: 수신자 멤버를 찾을 수 없음 (ownerId={})", event.ownerId());
            return;
        }

        log.info("[알림] 댓글 발생: {} -> {}", event.commenterNickname(), owner.getNickname());

        notificationService.sendCommentNotification(
                owner, // 상점 주인 (알림 받을 사람)
                event.commenterNickname(), // 작성자 닉네임
                event.storeId() // 상점 ID (링크 생성용)
                );
    }

    /** 구독 알림 SubscribeEvent(Member subscriber, Member target) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
