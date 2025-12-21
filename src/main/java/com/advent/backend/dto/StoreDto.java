package com.advent.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.web.bind.annotation.PostMapping;

public class StoreDto {
    // 1. [응답] 상점 정보 조회용
    @Getter
    @Builder
    @AllArgsConstructor
    public static class StoreResponse {
        @Schema(description = "상점 ID")
        private String id;

        @Schema(description = "상점 이름")
        private String name;
    }
}
