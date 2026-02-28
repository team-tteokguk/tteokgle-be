package com.advent.backend.entity;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

// 사용자
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {
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
    @Column(nullable = true, unique = true)
    private String nickname;

    // 프로필 이미지 URL
    @Column(nullable = true)
    private String profileImage;

    // 보유 엽전
    @Column(nullable = false)
    @Builder.Default
    private Integer point = 0;

    public static Member create(
            String nickname, SocialType socialType, String socialId, Integer point) {
        return Member.builder()
                .nickname(nickname)
                .socialType(socialType)
                .socialId(socialId)
                .point(point)
                .build();
    }

    // 서비스에서 사용할 업데이트 로직 (직접 작성)
    public Member updateNickname(String nickname) {
        this.nickname = nickname;
        return this; // 메서드 체이닝을 위해 자기 자신 반환
    }

    public Member updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
        return this;
    }

    public void addPoint(int amount) {
        if (amount < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.point += amount;
    }

    public void decreasePoint(int amount) {
        if (this.point < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.point -= amount;
    }
}
