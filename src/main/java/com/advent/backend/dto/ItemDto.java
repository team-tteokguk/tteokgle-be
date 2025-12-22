package com.advent.backend.dto;

import com.advent.backend.entity.Item;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

public class ItemDto {
    // 공통 정보
    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static abstract class ItemBase {
        @Schema(description = "고명 ID")
        private String id;

        @Schema(description = "고명 이름")
        private String name;

        @Schema(description = "고명 이미지")
        private String imageUrl;
    }

    // RESPONSE
    // 1. [응답] 삼정용 고명 정보 조회
    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreItemResponse extends ItemBase {
        @Schema(description = "고명 가격")
        private int cost;

        @Schema(description = "판매 수량")
        private int sellCounts;
    }

//    // 2. [응답] 나의 떡국용 배치된 고명 정보 조회
//    @Getter
//    @SuperBuilder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class PlacedItemResponse extends ItemBase {
//
//    }
//
//    // 3. [응답] 나의 떡국용 미배치된 고명 정보 조회
//    @Getter
//    @SuperBuilder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class NoPlacedItemResponse extends ItemBase {
//
//    }
//
//    // 4. [응답] 나의 떡국에서 고명 컨텐츠 조회
//    @Getter
//    @SuperBuilder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class ItemContentResponse extends ItemBase {
//
//    }

    // REQUEST
    // 1. [요청] 고명 정보 등록용
    @Getter
    @NoArgsConstructor
    public static class ItemCreateRequest {
        @Schema(description = "고명 이름")
        private String name;

        @Schema(description = "고명 이미지")
        private String imageUrl;

        @Schema(description = "컨텐츠 타입")
        private Item.ContentType contentType;

        @Schema(description = "컨텐츠 내용")
        private String mediaUrl;

        @Schema(description = "본문 내용")
        private String content;
    }
}
