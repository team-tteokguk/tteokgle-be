package com.advent.backend.dto;

import com.advent.backend.enums.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class AuthDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "로그인 응답")
    public static class LoginResponse {
        @Schema(description = "액세스 토큰")
        private String accessToken;

        @Schema(description = "리프레시 토큰")
        private String refreshToken;

        @Schema(description = "신규 가입 여부")
        private boolean isNewMember;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "토큰 재발급 응답")
    public static class TokenRefreshResponse {
        @Schema(description = "새로운 액세스 토큰")
        private String accessToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "토큰 재발급 요청")
    public static class RefreshTokenRequest {
        @Schema(description = "리프레시 토큰")
        private String refreshToken;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "로그인 요청")
    public static class LoginRequest {

        @Schema(description = "소셜 제공자 (kakao, google)")
        private SocialProvider provider;

        @Schema(description = "소셜 인증 코드")
        private String code;
    }
}
