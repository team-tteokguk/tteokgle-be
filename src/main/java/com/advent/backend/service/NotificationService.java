package com.advent.backend.service;

import com.advent.backend.dto.NotificationDto;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Notification;
import com.advent.backend.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final long SSE_TIMEOUT = 60L * 60L * 1000L;
    private static final String EVENT_CONNECTED = "connected";
    private static final String EVENT_NOTIFICATION = "notification";

    private final NotificationRepository notificationRepository;
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

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

        Notification saved = notificationRepository.save(notification);
        if (saved == null) {
            saved = notification;
        }
        sendToClient(receiver.getId(), NotificationDto.NotificationResponse.from(saved));
    }

    @Transactional
    public void sendSaleNotification(Member seller, String buyerNickname, String itemName) {
        String message = String.format("%s님이 회원님의 [%s]을(를) 구매했습니다! 💰", buyerNickname, itemName);
        String link = "/my-store/sales";

        saveNotification(seller, Notification.NotificationType.SALE, message, link);
    }

    @Transactional
    public void sendCommentNotification(Member owner, String writerNickname, UUID storeId) {
        String message = String.format("%s님이 상점에 방명록을 남겼습니다. 📝", writerNickname);
        String link = "/stores/" + storeId;

        saveNotification(owner, Notification.NotificationType.COMMENT, message, link);
    }

    @Transactional
    public void sendSubscribeNotification(Member owner, String subscriberNickname) {
        String message = String.format("%s님이 회원님의 상점을 구독했습니다! 🎉", subscriberNickname);
        String link = "/my-store/subscribers";

        saveNotification(owner, Notification.NotificationType.SUBSCRIBE, message, link);
    }

    @Transactional
    public void sendSystemNotification(Member receiver, String customMessage, String customLink) {
        saveNotification(receiver, Notification.NotificationType.ALARM, customMessage, customLink);
    }

    @Transactional(readOnly = true)
    public Slice<NotificationDto.NotificationResponse> getNotifications(
            UUID memberId, Pageable pageable) {
        return notificationRepository
                .findAllByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(NotificationDto.NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Member member) {
        return notificationRepository.findAllByMemberOrderByCreatedAtDesc(member);
    }

    @Transactional
    public void readNotification(UUID memberId) {
        notificationRepository.markAllAsReadByMemberId(memberId);
    }

    public SseEmitter subscribe(UUID memberId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(memberId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(memberId, emitter));
        emitter.onTimeout(() -> removeEmitter(memberId, emitter));
        emitter.onError(ex -> removeEmitter(memberId, emitter));

        try {
            emitter.send(SseEmitter.event().name(EVENT_CONNECTED).data("SSE connected"));
        } catch (Exception e) {
            removeEmitter(memberId, emitter);
        }

        return emitter;
    }

    private void sendToClient(UUID memberId, NotificationDto.NotificationResponse payload) {
        List<SseEmitter> memberEmitters = emitters.get(memberId);
        if (memberEmitters == null || memberEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : memberEmitters) {
            try {
                SseEmitter.SseEventBuilder eventBuilder =
                        SseEmitter.event().name(EVENT_NOTIFICATION).data(payload);
                if (payload.getId() != null) {
                    eventBuilder.id(payload.getId());
                }
                emitter.send(eventBuilder);
            } catch (Exception e) {
                removeEmitter(memberId, emitter);
                log.debug("SSE emitter removed: {}", e.getMessage());
            }
        }
    }

    private void removeEmitter(UUID memberId, SseEmitter emitter) {
        List<SseEmitter> memberEmitters = emitters.get(memberId);
        if (memberEmitters == null) {
            return;
        }
        memberEmitters.remove(emitter);
        if (memberEmitters.isEmpty()) {
            emitters.remove(memberId);
        }
    }
}
