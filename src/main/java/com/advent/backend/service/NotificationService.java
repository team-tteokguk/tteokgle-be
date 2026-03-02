package com.advent.backend.service;

import com.advent.backend.dto.NotificationDto;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Notification;
import com.advent.backend.repository.NotificationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
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

    @Value("${notification.retention-days:10}")
    private long retentionDays;

    @Value("${notification.max-per-member:200}")
    private int maxPerMember;

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
        log.info(
                "[알림] 저장 완료: receiverId={}, type={}, emitterCount={}",
                receiver.getId(),
                type,
                emitters.getOrDefault(receiver.getId(), new CopyOnWriteArrayList<>()).size());
        sendToClient(receiver.getId(), NotificationDto.NotificationResponse.from(saved));
    }

    @Transactional
    public void sendSaleNotification(Member seller, String buyerNickname, String itemName) {
        String message = String.format("%s님이 회원님의 [%s]을(를) 구매했습니다! 💰", buyerNickname, itemName);
        String link = "/my-store/sales";

        saveNotification(seller, Notification.NotificationType.SALE, message, link);
    }

    @Transactional
    public void sendPurchaseNotification(Member buyer, String sellerNickname, String itemName) {
        String message = String.format("%s님의 상점에서 [%s]을(를) 구매했습니다. 🛍️", sellerNickname, itemName);
        String link = "/tteokguk/me/items/unplaced";

        saveNotification(buyer, Notification.NotificationType.SALE, message, link);
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
        List<SseEmitter> staleEmitters = new ArrayList<>();
        emitters.compute(
                memberId,
                (key, currentEmitters) -> {
                    if (currentEmitters != null && !currentEmitters.isEmpty()) {
                        staleEmitters.addAll(currentEmitters);
                    }
                    CopyOnWriteArrayList<SseEmitter> refreshedEmitters =
                            new CopyOnWriteArrayList<>();
                    refreshedEmitters.add(emitter);
                    return refreshedEmitters;
                });
        staleEmitters.forEach(this::completeEmitterQuietly);

        if (!staleEmitters.isEmpty()) {
            log.info(
                    "[SSE] 기존 연결 교체: memberId={}, replacedCount={}",
                    memberId,
                    staleEmitters.size());
        }
        log.info(
                "[SSE] 구독 연결: memberId={}, emitterCount={}",
                memberId,
                emitters.get(memberId).size());

        emitter.onCompletion(() -> removeEmitter(memberId, emitter));
        emitter.onTimeout(() -> removeEmitter(memberId, emitter));
        emitter.onError(ex -> removeEmitter(memberId, emitter));

        try {
            emitter.send(SseEmitter.event().name(EVENT_CONNECTED).data("SSE connected"));
            emitter.send(SseEmitter.event().name("debug-member-id").data(memberId.toString()));
        } catch (Exception e) {
            removeEmitter(memberId, emitter);
        }

        return emitter;
    }

    private void sendToClient(UUID memberId, NotificationDto.NotificationResponse payload) {
        List<SseEmitter> memberEmitters = emitters.get(memberId);
        if (memberEmitters == null || memberEmitters.isEmpty()) {
            log.info("[SSE] 전송 스킵(연결 없음): memberId={}", memberId);
            return;
        }

        log.info("[SSE] 전송 시도: memberId={}, emitterCount={}", memberId, memberEmitters.size());
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

    private void completeEmitterQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // no-op
        }
    }

    private void removeEmitter(UUID memberId, SseEmitter emitter) {
        List<SseEmitter> memberEmitters = emitters.get(memberId);
        if (memberEmitters == null) {
            return;
        }
        memberEmitters.remove(emitter);
        log.info("[SSE] 연결 해제: memberId={}, emitterCount={}", memberId, memberEmitters.size());
        if (memberEmitters.isEmpty()) {
            emitters.remove(memberId);
        }
    }

    @Scheduled(cron = "${notification.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void cleanupNotifications() {
        var threshold = java.time.LocalDateTime.now().minusDays(retentionDays);
        var oldIds = notificationRepository.findIdsCreatedBefore(threshold);
        var overflowIds = notificationRepository.findIdsExceedingLimitPerMember(maxPerMember);

        if (!oldIds.isEmpty()) {
            notificationRepository.deleteAllByIdInBatch(oldIds);
        }
        if (!overflowIds.isEmpty()) {
            notificationRepository.deleteAllByIdInBatch(overflowIds);
        }

        if (!oldIds.isEmpty() || !overflowIds.isEmpty()) {
            log.info(
                    "[알림] 정리 완료: oldDeleted={}, overflowDeleted={}, retentionDays={}, maxPerMember={}",
                    oldIds.size(),
                    overflowIds.size(),
                    retentionDays,
                    maxPerMember);
        }
    }
}
