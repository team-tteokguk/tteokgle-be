package com.advent.backend.service;

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

    // 알림 생성 (저장)
    @Transactional
    public void createNotification(
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

    // 내 알림 조회
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Member member) {
        return notificationRepository.findAllByMemberOrderByCreatedAtDesc(member);
    }

    // 알림 읽음 처리
    @Transactional
    public void readNotification(UUID id) {
        notificationRepository.updateNotification(id);
    }
}
