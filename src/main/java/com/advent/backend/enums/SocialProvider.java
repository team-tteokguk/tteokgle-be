package com.advent.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소셜 로그인 제공자")
public enum SocialProvider {

    @Schema(description = "카카오 로그인")
    KAKAO("kakao"),

    @Schema(description = "구글 로그인")
    GOOGLE("google");

    private final String value;

    SocialProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SocialProvider fromString(String value) {
        for (SocialProvider provider : SocialProvider.values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        // 잘못된 값이 들어오면 예외 발생 (GlobalExceptionHandler에서 처리)
        throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + value);
    }
}
