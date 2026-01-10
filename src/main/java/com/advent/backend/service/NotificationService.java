package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Notification;
import com.advent.backend.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 공통 알림 메서드
     *
     * @param receiver
     * @param type
     * @param message
     * @param link
     */
    @Transactional
    protected void saveNotification(
            Member receiver, Notification.NotificationType type, String message, String link) {
        Notification notification =
                Notification.builder()
                        .member(receiver)
                        .notificationType(type)
                        .message(message)
                        .link(link)
                        .isRead(false)
                        .build();

        notificationRepository.save(notification);
    }

    /**
     * 판매 알림
     *
     * @param seller
     * @param buyerNickname
     * @param itemName
     */
    @Transactional
    public void sendSaleNotification(Member seller, String buyerNickname, String itemName) {
        String message = String.format("%s님이 회원님의 [%s]을(를) 구매했습니다! 💰", buyerNickname, itemName);
        String link = "/my-store/sales";

        saveNotification(seller, Notification.NotificationType.SALE, message, link);
    }

    /**
     * 방명록 알림
     *
     * @param owner
     * @param writerNickname
     * @param storeId
     */
    @Transactional
    public void sendCommentNotification(Member owner, String writerNickname, UUID storeId) {
        String message = String.format("%s님이 상점에 방명록을 남겼습니다. 📝", writerNickname);
        String link = "/stores/" + storeId.toString();

        saveNotification(owner, Notification.NotificationType.COMMENT, message, link);
    }

    /**
     * 상점 즐겨찾기 알림
     *
     * @param owner
     * @param subscriberNickname
     */
    @Transactional
    public void sendSubscribeNotification(Member owner, String subscriberNickname) {
        String message = String.format("%s님이 회원님의 상점을 구독했습니다! 🎉", subscriberNickname);
        String link = "/my-store/subscribers";

        saveNotification(owner, Notification.NotificationType.SUBSCRIBE, message, link);
    }

    /**
     * 전체 공지 / 시스템 알림
     *
     * @param receiver
     * @param customMessage
     * @param customLink
     */
    @Transactional
    public void sendSystemNotification(Member receiver, String customMessage, String customLink) {
        saveNotification(receiver, Notification.NotificationType.ALARM, customMessage, customLink);
    }

    /**
     * 알림 목록 조회
     *
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Member member) {
        return notificationRepository.findAllByMemberOrderByCreatedAtDesc(member);
    }

    /**
     * 알림 읽음 처리
     *
     * @param notificationId
     */
    @Transactional
    public void readNotification(UUID notificationId) {
        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.read();
    }
}
