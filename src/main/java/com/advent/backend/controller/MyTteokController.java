package com.advent.backend.controller;

import com.advent.backend.dto.ItemDto;
import com.advent.backend.security.CustomUserDetails;
import com.advent.backend.service.MyTteokService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyTteok", description = "나의 떡국 (고명 배치/조회) API")
@RestController
@RequestMapping("/tteokguk/me/items")
@RequiredArgsConstructor
public class MyTteokController {

    private final MyTteokService myTteokService;

    @Operation(summary = "배치된 고명 리스트 조회 (떡국 위)")
    @GetMapping("/placed")
    public ResponseEntity<ItemDto.PlacedItemSliceResponse> getPlacedItems(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(size = 9) Pageable pageable) {
        return ResponseEntity.ok(
                myTteokService.getPlacedItems(customUserDetails.getMember(), pageable));
    }

    @Operation(summary = "인벤토리(바텀시트) 고명 리스트 조회")
    @GetMapping("/unplaced")
    public ResponseEntity<ItemDto.UnplacedItemSliceResponse> getUnplacedItems(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(size = 9) Pageable pageable) {
        return ResponseEntity.ok(
                myTteokService.getUnplacedItems(customUserDetails.getMember(), pageable));
    }

    @Operation(summary = "고명 배치 및 수납 (좌표/상태 수정)")
    @PatchMapping("/{itemId}")
    public ResponseEntity<Void> updateItemPlacement(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable UUID itemId,
            @RequestBody(required = false) ItemDto.ItemPlacementRequest request) {
        myTteokService.updateItemPlacement(customUserDetails.getMember(), itemId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "고명 컨텐츠(편지/사진) 상세 조회")
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDto.ItemDetailResponse> getItemDetail(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(
                myTteokService.getItemDetail(customUserDetails.getMember(), itemId));
    }

    @Operation(summary = "읽지 않은 고명 읽음 처리")
    @PatchMapping("/{itemId}/read")
    public ResponseEntity<Void> readItem(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable UUID itemId) {
        myTteokService.readItem(customUserDetails.getMember(), itemId);
        return ResponseEntity.ok().build();
    }
}
