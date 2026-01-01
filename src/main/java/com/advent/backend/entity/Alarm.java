package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 알림
@Entity
@Table(name = "alarms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm extends BaseTimeEntity {

    public enum AlarmType {
        COMMENT,
        SALE,
        SUBSCRIBE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Member sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmType alarmType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "is_read")
    private boolean isRead = false;
}
