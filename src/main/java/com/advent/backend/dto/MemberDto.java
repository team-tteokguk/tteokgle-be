package com.advent.backend.dto;

import com.advent.backend.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

public class MemberDto {

    /** 1. [응답] 마이페이지용 */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "회원 정보 응답")
    public static class MemberMyPageResponse {
        @Schema(description = "닉네임")
        private String nickname;

        @Schema(description = "소셜 타입 (KAKAO, GOOGLE)")
        private String socialType;

        @Schema(description = "보유 엽전")
        private int point;

        @Schema(description = "가입일")
        private String createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "회원 정보 응답")
    public static class MemberResponse {
        @Schema(description = "내부 관리용 식별자")
        private UUID memberId;

        @Schema(description = "사용자 식별용 ID")
        private String socailId;

        @Schema(description = "소셜 타입 (KAKAO, GOOGLE)")
        private Member.SocialType socialType;

        @Schema(description = "닉네임")
        private String nickname;

        @Schema(description = "보유 엽전")
        private int point;

        @Schema(description = "가입일")
        private LocalDateTime createdAt;

        public static MemberDto.MemberResponse from(Member member) {
            return MemberResponse.builder()
                    .memberId(member.getId())
                    .socailId(member.getSocialId())
                    .nickname(member.getNickname())
                    .point(member.getPoint())
                    .socialType(member.getSocialType())
                    .createdAt(member.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "닉네임 변경 요청")
    public static class NicknameUpdateRequest {
        @Schema(description = "새 닉네임", example = "고명_97")
        private String nickname;
    }
}
