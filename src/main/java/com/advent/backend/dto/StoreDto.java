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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StoreSummaryResponse {
        @Schema(description = "상점 ID")
        private UUID storeId;

        @Schema(description = "상점 주인 닉네임")
        private String nickname;

        @Schema(description = "상점 이름")
        private String storeName;

        @Schema(description = "상점 주인 프로필 이미지 URL")
        private String profileImage;

        @Schema(description = "판매 중인 고명 종류 개수")
        private long sellingItemTypeCount;

        @Schema(description = "로그인 사용자의 즐겨찾기 여부")
        private boolean favorite;
    }
}
