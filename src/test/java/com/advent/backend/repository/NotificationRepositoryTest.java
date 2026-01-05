package com.advent.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.advent.backend.entity.Member;
import com.advent.backend.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class NotificationRepositoryTest {
    @Autowired private NotificationRepository notificationRepository;

    @Autowired private MemberRepository memberRepository;

    @Autowired private TestEntityManager entityManager; // 혹은 EntityManager

    @Test
    @DisplayName("알림 생성")
    void should_SaveNotificaion() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member =
                Member.builder().socialId(socialId).socialType(socialType).nickname("외요3").build();

        memberRepository.save(member);

        Notification notification =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.COMMENT)
                        .message("네 고명에 큰 인상을 받았어")
                        .build();

        notificationRepository.saveAndFlush(notification);

        Optional<Notification> result = notificationRepository.findById(notification.getId());

        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(notification);
        assertThat(result.get().getNotificationType())
                .isEqualTo(Notification.NotificationType.COMMENT);
        assertThat(result.get().getMessage()).isEqualTo(notification.getMessage());
        assertThat(result.get().getId()).isEqualTo(notification.getId());
    }

    @Test
    @DisplayName("모든 알림 한 번에 읽음 처리")
    void should_UpdateAllIsReadToTrue() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member =
                Member.builder().socialId(socialId).socialType(socialType).nickname("외요3").build();

        memberRepository.save(member);

        Notification notification =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.COMMENT)
                        .message("네 고명에 큰 인상을 받았어")
                        .build();

        Notification notification2 =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.SALE)
                        .message("a회원이 님 고명을 구매했어요")
                        .build();

        Notification notification3 =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.SUBSCRIBE)
                        .message("a회원이 님 상점을 구독했어요")
                        .build();

        Notification notification4 =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.SUBSCRIBE)
                        .message("전체 회원들한테 공지 드림")
                        .build();

        notificationRepository.saveAllAndFlush(
                List.of(notification, notification2, notification3, notification4));

        //        entityManager.flush();

        notificationRepository.updateNotification(member.getId());
        List<Notification> allNotifications = notificationRepository.findAll();
        assertThat(allNotifications).allMatch(Notification::isRead);
    }

    @Test
    @DisplayName("페이징으로 10페이지씩 알림 조회")
    void should_FindPagedNotifications() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member =
                Member.builder().socialId(socialId).socialType(socialType).nickname("외요3").build();

        memberRepository.save(member);

        Notification notification =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.COMMENT)
                        .message("네 고명에 큰 인상을 받았어")
                        .build();

        Notification notification2 =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.SALE)
                        .message("a회원이 님 고명을 구매했어요")
                        .build();

        Notification notification3 =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.SUBSCRIBE)
                        .message("a회원이 님 상점을 구독했어요")
                        .build();

        Notification notification4 =
                Notification.builder()
                        .member(member)
                        .notificationType(Notification.NotificationType.ALARM)
                        .message("전체 회원들한테 공지 드림")
                        .build();

        notificationRepository.saveAll(
                List.of(notification, notification2, notification3, notification4));

        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        Page<Notification> page = notificationRepository.findAll(pageable);

        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
    }
}
