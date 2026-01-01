package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

// 알림
@Entity
@Table(name = "notifications")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    public enum NotificationType {
        COMMENT, // 방명록 알림
        SALE, // 판매 알림
        SUBSCRIBE, // 구독 알림
        ALARM // 전체 공지
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType NotificationType;

    @Column(name = "link")
    private String link;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;
}
