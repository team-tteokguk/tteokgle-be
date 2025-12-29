package com.advent.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

// 사용자
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {
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
    private String socialId;

    // 유저 닉네임, 8자까지 (최소 글자는 비지니스 로직에서 판단)
    @Column(nullable = false, length = 8)
    private String nickname;

    // 보유 엽전
    @Column(columnDefinition = "integer default 0", nullable = false)
    private Integer point = 0;

    // 서비스에서 사용할 업데이트 로직 (직접 작성)
    public Member updateNickname(String nickname) {
        this.nickname = nickname;
        return this; // 메서드 체이닝을 위해 자기 자신 반환
    }
}
