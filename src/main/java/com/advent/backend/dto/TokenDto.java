package com.advent.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class TokenDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long accessTokenExpiresIn;
    }
}
