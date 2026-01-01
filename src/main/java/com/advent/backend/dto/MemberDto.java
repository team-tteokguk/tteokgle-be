package com.advent.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class MemberDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "회원 정보 응답")
    public static class MemberResponse {
        @Schema(description = "회원 ID")
        private String userId;

        @Schema(description = "이메일")
        private String email;

        @Schema(description = "소셜 타입 (KAKAO, GOOGLE)")
        private String socialType;

        @Schema(description = "닉네임")
        private String nickname;

        @Schema(description = "보유 엽전")
        private int point;

        @Schema(description = "가입일")
        private String createdAt;
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
