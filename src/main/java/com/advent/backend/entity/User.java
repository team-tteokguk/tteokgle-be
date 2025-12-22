package com.advent.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 사용자
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    public enum SocialType {
        NONE,
        KAKAO,
        GOOGLE
    }

    // 유저 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 소셜 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialType socialType;

    // 소셜 식별값
    @Column(nullable = false)
    private String social_id;

    // 유저 닉네임, 8자까지 (최소 글자는 비지니스 로직에서 판단)
    @Column(nullable = false, length = 8)
    private String nickname;

    // 보유 엽전
    @Column(columnDefinition = "integer default 0", nullable = false)
    private Integer point = 0;
}
