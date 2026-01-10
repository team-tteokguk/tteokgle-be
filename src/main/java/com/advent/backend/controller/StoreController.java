package com.advent.backend.controller;

import com.advent.backend.dto.GuestBookDto;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.dto.StoreDto;
import com.advent.backend.security.CustomUserDetails;
import com.advent.backend.service.GuestBookService;
import com.advent.backend.service.StoreService;
import com.advent.backend.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상점 관리", description = "상점 정보 조회 및 고명 조회, 수정, 등록")
@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;
    private final GuestBookService guestBookService;
    private final SubscriptionService subscriptionService;

    // 상점 API
    // 1. 상점 정보 불러오기
    @Operation(summary = "상점 정보", description = "특정 상점의 정보를 불러옵니다.")
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreDto.StoreResponse> getStoreInfo(@PathVariable UUID storeId) {
        return ResponseEntity.ok(storeService.getStoreInfo(storeId));
    }

    // 2. 고명 리스트 조회하기
    @Operation(summary = "고명 리스트 조회하기")
    @GetMapping("/{storeId}/items")
    public ResponseEntity<Slice<ItemDto.StoreItemResponse>> getItems(
            @PathVariable UUID storeId, @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(storeService.getItems(storeId, pageable));
    }

    // 3. 고명 등록하기
    // 등록한 고명을 고명 리스트에 바로 업데이트 (고명 리스트 API를 한 번 더 호출하지 않음)
    @Operation(summary = "고명 등록하기")
    @PostMapping("/{storeId}/items")
    public ResponseEntity<ItemDto.StoreItemResponse> createItem(
            @PathVariable UUID storeId, @RequestBody ItemDto.ItemCreateRequest itemCreateRequest) {
        ItemDto.StoreItemResponse item = storeService.publishItem(storeId, itemCreateRequest);
        return ResponseEntity.ok(item);
    }

    // 4. 고명 삭제하기
    @Operation(summary = "고명 삭제하기")
    @DeleteMapping("/{storeId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID storeId, @PathVariable UUID itemId) {
        storeService.removeItem(storeId, itemId);
        return ResponseEntity.noContent().build();
    }

    // 방명록 API
    // 1. 방명록 불러오기
    @Operation(summary = "방명록 불러오기", description = "특정 상점의 방명록을 불러옵니다.")
    @GetMapping("/{storeId}/guestbooks")
    public ResponseEntity<Slice<GuestBookDto.GuestBookResponse>> getGuestBookInfo(
            @PathVariable UUID storeId, @PageableDefault(size = 10) Pageable pageable) {
        guestBookService.getGuestBooks(storeId, pageable);

        // 추후 Slice
        return ResponseEntity.ok().build();
    }

    // 2. 방명록 작성하기
    @Operation(summary = "방명록 작성하기")
    @PostMapping("/{storeId}/guestbooks")
    public ResponseEntity<GuestBookDto.GuestBookResponse> createGuestBook(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody GuestBookDto.GuestBookRequest guestBookRequest) {
        //        guestBookService.createGuestBook(storeId, customUserDetails.getMember(),
        // guestBookRequest)

        return ResponseEntity.ok().build();
    }

    // 3. 방명록 수정하기
    @Operation(summary = "방명록 수정하기")
    @PatchMapping("/{storeId}/guestbooks/{guestbookId}")
    public ResponseEntity<GuestBookDto.GuestBookResponse> updateGuestBook(
            @PathVariable UUID guestbookId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody GuestBookDto.GuestBookRequest request) {
        guestBookService.updateGuestBook(guestbookId, customUserDetails.getMember(), request);

        return ResponseEntity.ok().build();
    }

    // 4. 방명록 삭제하기
    @Operation(summary = "방명록 삭제하기")
    @DeleteMapping("/{storeId}/guestbooks/{guestbookId}")
    public ResponseEntity<Void> deleteGuestBook(
            @PathVariable UUID guestbookId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        guestBookService.deleteGuestBook(guestbookId, customUserDetails.getMember());

        return ResponseEntity.noContent().build();
    }

    // 즐겨찾기
    /**
     * 즐겨찾기에 추가 (구독 추가)
     *
     * @param storeId
     * @param customUserDetails
     * @return
     */
    @Operation(summary = "즐겨찾기 추가하기")
    @PostMapping("/{storeId}/subscription")
    public ResponseEntity<Void> subscribe(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        subscriptionService.subscribe(customUserDetails.getMember().getId(), storeId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 즐겨찾기에서 삭제 (구독 해제)
     *
     * @param storeId
     * @param customUserDetails
     * @return
     */
    @Operation(summary = "즐겨찾기 삭제하기")
    @DeleteMapping("/{storeId}/subscription")
    public ResponseEntity<Void> unSubscribe(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        subscriptionService.unsubscribe(customUserDetails.getMember().getId(), storeId);

        return ResponseEntity.noContent().build();
    }
}
