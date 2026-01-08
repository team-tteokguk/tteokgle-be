package com.advent.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class SubscriptionDto {
    // 1. [응답] 상점 정보 조회용
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SubscriptionResponse {
        @Schema(description = "구독 ID")
        private String id;

        @Schema(description = "구독한 상점 이름")
        private String storeName;

        @Schema(description = "구독한 상점 링크")
        private String storeUrl;

        @Schema(description = "구독한 상점 주인 이름")
        private String MemberName;
    }
}
