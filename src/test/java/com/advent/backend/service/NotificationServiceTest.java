package com.advent.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.advent.backend.entity.Member;
import com.advent.backend.entity.Notification;
import com.advent.backend.repository.NotificationRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks private NotificationService notificationService;

    @Mock private NotificationRepository notificationRepository;

    @Test
    @DisplayName("판매 알림을 보내면 메시지와 링크가 포맷에 맞춰 생성되어 저장된다")
    void should_SaveSaleNotification_With_CorrectFormat() {
        Member seller = Member.builder().id(UUID.randomUUID()).nickname("판매자").build();
        String buyerNickname = "구매자";
        String itemName = "맛있는 떡국";

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.sendSaleNotification(seller, buyerNickname, itemName);

        verify(notificationRepository).save(captor.capture());
        Notification savedNotification = captor.getValue();

        assertThat(savedNotification.getMember()).isEqualTo(seller);
        assertThat(savedNotification.getNotificationType())
                .isEqualTo(Notification.NotificationType.SALE);
        assertThat(savedNotification.getMessage()).contains("구매자", "맛있는 떡국", "구매했습니다");
        assertThat(savedNotification.getLink()).isEqualTo("/my-store/sales");
        assertThat(savedNotification.isRead()).isFalse();
    }

    @Test
    @DisplayName("방명록 알림을 보내면 상점 ID가 포함된 링크가 생성된다")
    void should_SaveCommentNotification_With_StoreLink() {
        Member owner = Member.builder().id(UUID.randomUUID()).build();
        String writerNickname = "악플러";
        UUID storeId = UUID.randomUUID();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.sendCommentNotification(owner, writerNickname, storeId);

        verify(notificationRepository).save(captor.capture());
        Notification savedNotification = captor.getValue();

        assertThat(savedNotification.getNotificationType())
                .isEqualTo(Notification.NotificationType.COMMENT);
        assertThat(savedNotification.getMessage()).contains("악플러", "방명록");
        assertThat(savedNotification.getLink()).isEqualTo("/stores/" + storeId);
    }

    @Test
    @DisplayName("구독 알림을 보내면 구독자 목록 링크가 생성된다")
    void should_SaveSubscribeNotification_With_SubscriberLink() {
        Member owner = Member.builder().id(UUID.randomUUID()).build();
        String subscriberNickname = "팬클럽회장";

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.sendSubscribeNotification(owner, subscriberNickname);

        verify(notificationRepository).save(captor.capture());
        Notification savedNotification = captor.getValue();

        assertThat(savedNotification.getNotificationType())
                .isEqualTo(Notification.NotificationType.SUBSCRIBE);
        assertThat(savedNotification.getMessage()).contains("팬클럽회장", "구독");
        assertThat(savedNotification.getLink()).isEqualTo("/my-store/subscribers");
    }

    @Test
    @DisplayName("내 알림 목록을 최신순으로 조회한다")
    void should_ReturnNotificationList() {
        Member member = Member.builder().id(UUID.randomUUID()).build();
        List<Notification> mockList =
                List.of(
                        Notification.builder().message("알림1").build(),
                        Notification.builder().message("알림2").build());

        given(notificationRepository.findAllByMemberOrderByCreatedAtDesc(member))
                .willReturn(mockList);

        List<Notification> result = notificationService.getNotifications(member);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMessage()).isEqualTo("알림1");
    }

    @Test
    @DisplayName("모든 알림을 일괄 읽음 처리한다")
    void should_MarkAllNotificationsAsRead() {
        UUID memberId = UUID.randomUUID();

        notificationService.readNotification(memberId);

        verify(notificationRepository).markAllAsReadByMemberId(memberId);
    }

    @Test
    @DisplayName("SSE 재구독 시 기존 연결을 교체해 단일 연결만 유지한다")
    void should_KeepSingleEmitter_WhenResubscribed() throws Exception {
        UUID memberId = UUID.randomUUID();

        SseEmitter firstEmitter = notificationService.subscribe(memberId);
        SseEmitter secondEmitter = notificationService.subscribe(memberId);

        Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = getEmitters(notificationService);

        assertThat(emitters.get(memberId)).hasSize(1);
        assertThat(emitters.get(memberId)).contains(secondEmitter);
        assertThat(emitters.get(memberId)).doesNotContain(firstEmitter);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, CopyOnWriteArrayList<SseEmitter>> getEmitters(
            NotificationService service) throws Exception {
        Field field = NotificationService.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (Map<UUID, CopyOnWriteArrayList<SseEmitter>>) field.get(service);
    }
}
