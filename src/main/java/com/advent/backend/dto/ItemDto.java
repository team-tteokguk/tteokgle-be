package com.advent.backend.dto;

import com.advent.backend.entity.Item;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Slice;

public class ItemDto {
    // 공통 정보
    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class ItemBase {
        @Schema(description = "고명 ID")
        private UUID id;

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

        @Schema(description = "로그인 유저 구매 여부")
        @JsonProperty("isPurchased")
        private boolean isPurchased;

        public static StoreItemResponse from(Item item) {
            return from(item, false);
        }

        public static StoreItemResponse from(Item item, boolean isPurchased) {
            return StoreItemResponse.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .imageUrl(item.getImageUrl())
                    .cost(item.getCost())
                    .sellCounts(item.getQuantity())
                    .isPurchased(isPurchased)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SliceInfo {
        @Schema(description = "현재 페이지 번호(0-base)")
        private int page;

        @Schema(description = "요청한 페이지 크기")
        private int size;

        @Schema(description = "현재 페이지 데이터 개수")
        private int numberOfElements;

        @Schema(description = "다음 페이지 존재 여부")
        private boolean hasNext;

        @Schema(description = "첫 페이지 여부")
        private boolean first;

        public static SliceInfo from(Slice<?> slice) {
            return SliceInfo.builder()
                    .page(slice.getNumber())
                    .size(slice.getSize())
                    .numberOfElements(slice.getNumberOfElements())
                    .hasNext(slice.hasNext())
                    .first(slice.isFirst())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreItemSliceResponse {
        @Schema(description = "상점 이름")
        private String storeName;

        @Schema(description = "판매중 고명 개수")
        private long sellingItemCount;

        @Schema(description = "고명 목록")
        private List<StoreItemResponse> items;

        @Schema(description = "페이지 정보")
        private SliceInfo page;

        public static StoreItemSliceResponse of(
                String storeName, long sellingItemCount, Slice<StoreItemResponse> itemSlice) {
            return StoreItemSliceResponse.builder()
                    .storeName(storeName)
                    .sellingItemCount(sellingItemCount)
                    .items(itemSlice.getContent())
                    .page(SliceInfo.from(itemSlice))
                    .build();
        }
    }

    // 2. [응답] 나의 떡국용 배치된 고명 정보 조회
    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlacedItemResponse extends ItemBase {
        @Schema(description = "배치 X 좌표")
        private Float posX;

        @Schema(description = "배치 Y 좌표")
        private Float posY;

        @Schema(description = "배치 Z 좌표")
        private Float posZ;

        @Schema(description = "사용 여부")
        private boolean isUsed;

        @Schema(description = "읽음 여부")
        private boolean isRead;
    }

    // 3. [응답] 나의 떡국용 미배치된 고명 정보 조회
    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnplacedItemResponse extends ItemBase {
        @Schema(description = "컨텐츠 확인 여부")
        private boolean isRead;

        @Schema(description = "배치 여부")
        private boolean isUsed;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlacedItemSliceResponse {
        @Schema(description = "배치된 고명 목록")
        private List<PlacedItemResponse> items;

        @Schema(description = "페이지 정보")
        private SliceInfo page;

        public static PlacedItemSliceResponse from(Slice<PlacedItemResponse> itemSlice) {
            return PlacedItemSliceResponse.builder()
                    .items(itemSlice.getContent())
                    .page(SliceInfo.from(itemSlice))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnplacedItemSliceResponse {
        @Schema(description = "미배치 고명 목록")
        private List<UnplacedItemResponse> items;

        @Schema(description = "페이지 정보")
        private SliceInfo page;

        public static UnplacedItemSliceResponse from(Slice<UnplacedItemResponse> itemSlice) {
            return UnplacedItemSliceResponse.builder()
                    .items(itemSlice.getContent())
                    .page(SliceInfo.from(itemSlice))
                    .build();
        }
    }

    // 4. [응답] 나의 떡국에서 고명 컨텐츠 조회
    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDetailResponse extends ItemBase {
        @Schema(description = "고명을 만든 사람 닉네임")
        private String creatorNickname;

        @Schema(description = "고명 종류", example = "scallion")
        private String itemType;

        @Schema(description = "컨텐츠 타입")
        private String contentType;

        @Schema(description = "컨텐츠 내용")
        private String mediaUrl;

        @Schema(description = "본문 메시지")
        private String content;

        @Schema(description = "읽음 여부")
        private boolean isRead;
    }

    // REQUEST
    // 1. [요청] 고명 정보 등록용
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
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

        @Schema(description = "재고 수량", example = "5")
        @JsonProperty("sellCounts")
        @JsonAlias({"sellCount", "quantity", "stock"})
        private Integer sellCounts;
    }

    @Getter
    @lombok.Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "고명 배치 상태 수정 요청")
    public static class ItemPlacementRequest {
        @Schema(description = "배치 여부")
        @JsonProperty("isUsed")
        @JsonAlias({"used", "is_used"})
        private Boolean isUsed;

        @Schema(description = "좌표 X")
        @JsonAlias({"pos_x", "x"})
        private Float posX;

        @Schema(description = "좌표 Y")
        @JsonAlias({"pos_y", "y"})
        private Float posY;

        @Schema(description = "좌표 Z")
        @JsonAlias({"pos_z", "z"})
        private Float posZ;
    }
}
