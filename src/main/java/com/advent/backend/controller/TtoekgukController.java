package com.advent.backend.controller;

import com.advent.backend.dto.ItemDto;
import com.advent.backend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "나의 떡국", description = "배치된 고명 리스트 조회, 인벤토리 고명 리스트 조회, 고명 배치 및 수납, 고명 컨텐츠 조회")
@RestController
@RequestMapping("/ttoekguk")
@RequiredArgsConstructor
public class TtoekgukController {

    @Operation(summary = "배치된 고명 리스트 조회", description = "배치된 고명 리스트를 조회합니다.")
    @GetMapping("/me/items/placed")
    public ResponseEntity<List<ItemDto.PlacedItemResponse>> getPlacedItems(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "인벤토리 고명 리스트",
            description = "사용되지 않은 인벤토리 고명 리스트를 조회힙니다."
            // 사용 O, X 여부 상관없이 다 불러올 수도 있음.
            )
    @GetMapping("/me/items/unplaced")
    public ResponseEntity<List<ItemDto.UnplacedItemResponse>> getUnplacedList(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        ItemDto.UnplacedItemResponse response =
                ItemDto.UnplacedItemResponse.builder()
                        .id(UUID.randomUUID())
                        .name("고명2")
                        .imageUrl("https://...")
                        .isRead(false)
                        .build();

        return ResponseEntity.ok(List.of(response));
    }

    @Operation(summary = "고명 배치 및 수납", description = "고명의 좌표를 바꾸거나 배치 상태를 변경합니다.")
    @PatchMapping("/me/items/{itemId}")
    public ResponseEntity<ItemDto.PlacedItemResponse> updateItemPlacement(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable UUID itemId,
            @RequestBody ItemDto.ItemPlacementRequest request) {
        ItemDto.PlacedItemResponse response =
                ItemDto.PlacedItemResponse.builder()
                        .id(UUID.randomUUID())
                        .name("고명1")
                        .imageUrl(
                                "https://i.pinimg.com/1200x/22/76/e5/2276e5e6c7f236b18c74600f3b72902a.jpg")
                        .posX(110.00f)
                        .posY(121.00f)
                        .isUsed(true)
                        .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "고명 컨텐츠 조회", description = "고명 컨텐츠 세부사항을 조회합니다.")
    @GetMapping("/me/items/{itemId}")
    public ResponseEntity<ItemDto.ItemDetailResponse> getItemDetail(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable UUID itemId) {
        ItemDto.ItemDetailResponse response =
                ItemDto.ItemDetailResponse.builder()
                        .id(UUID.randomUUID())
                        .name("고명1")
                        .imageUrl(
                                "https://i.pinimg.com/1200x/22/76/e5/2276e5e6c7f236b18c74600f3b72902a.jpg")
                        .contentType("TEXT")
                        .mediaUrl(null)
                        .content("메리 크리스마스!")
                        .isRead(false)
                        .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "고명 읽음 처리", description = "읽은 적 없는 고명의 상태를 읽음 처리 합니다. (isRead = false)")
    @PatchMapping("/me/items/{itemId}/read")
    public ResponseEntity<Void> readItem(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "읽음 처리할 고명 ID", required = true) @PathVariable UUID itemId) {
        return ResponseEntity.ok().build();
    }
}
