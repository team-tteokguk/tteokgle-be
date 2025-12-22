package com.advent.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 방명록
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestBook extends BaseTimeEntity {
    // 방명록 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 유저 ID
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 200)
    private String content;
}
