package com.advent.backend.dto;

import com.advent.backend.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.*;

public class StoreDto {
    // 1. [응답] 상점 정보 조회용
    @Getter
    @Builder
    @AllArgsConstructor
    public static class StoreResponse {
        @Schema(description = "상점 ID")
        private UUID id;

        @Schema(description = "상점 이름")
        private String name;

        public static StoreResponse from(Store store) {
            return StoreResponse.builder().id(store.getId()).name(store.getTitle()).build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoreNameUpdateRequest {
        @Schema(description = "변경할 상점명")
        private String name;
    }
}
