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

    @Operation(summary = "내 상점 정보", description = "로그인한 회원의 상점 정보를 불러옵니다.")
    @GetMapping("/me")
    public ResponseEntity<StoreDto.StoreResponse> getMyStoreInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(
                storeService.getMyStoreInfo(customUserDetails.getMember().getId()));
    }

    @Operation(summary = "상점 검색", description = "다른 사람의 닉네임 또는 상점명으로 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<Slice<StoreDto.StoreSummaryResponse>> searchStores(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(
                storeService.searchStores(
                        customUserDetails.getMember().getId(), keyword, pageable));
    }

    // 2. 고명 리스트 조회하기
    @Operation(summary = "고명 리스트 조회하기")
    @GetMapping("/{storeId}/items")
    public ResponseEntity<ItemDto.StoreItemSliceResponse> getItems(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(
                storeService.getItems(customUserDetails.getMember().getId(), storeId, pageable));
    }

    @Operation(summary = "내 상점 고명 리스트 조회하기")
    @GetMapping("/me/items")
    public ResponseEntity<ItemDto.StoreItemSliceResponse> getMyItems(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(
                storeService.getMyItems(customUserDetails.getMember().getId(), pageable));
    }

    @Operation(summary = "내 상점명 변경")
    @RequestMapping(
            value = "/me",
            method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<Void> updateStoreName(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody StoreDto.StoreNameUpdateRequest request) {
        storeService.updateStoreName(customUserDetails.getMember().getId(), request.getName());
        return ResponseEntity.noContent().build();
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

    @Operation(summary = "고명 구매하기")
    @PostMapping("/items/{itemId}/purchase")
    public ResponseEntity<Void> purchaseItem(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        storeService.purchaseItem(customUserDetails.getMember().getId(), itemId);
        return ResponseEntity.noContent().build();
    }

    // 방명록 API
    // 1. 방명록 불러오기
    @Operation(summary = "방명록 불러오기", description = "특정 상점의 방명록을 불러옵니다.")
    @GetMapping("/{storeId}/guestbooks")
    public ResponseEntity<Slice<GuestBookDto.GuestBookResponse>> getGuestBookInfo(
            @PathVariable UUID storeId, @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(guestBookService.getGuestBooks(storeId, pageable));
    }

    // 2. 방명록 작성하기
    @Operation(summary = "방명록 작성하기")
    @PostMapping("/{storeId}/guestbooks")
    public ResponseEntity<GuestBookDto.GuestBookResponse> createGuestBook(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody GuestBookDto.GuestBookRequest guestBookRequest) {
        return ResponseEntity.ok(
                guestBookService.createGuestBook(
                        storeId, customUserDetails.getMember(), guestBookRequest));
    }

    // 3. 방명록 수정하기
    @Operation(summary = "방명록 수정하기")
    @RequestMapping(
            value = "/{storeId}/guestbooks/{guestbookId}",
            method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<GuestBookDto.GuestBookResponse> updateGuestBook(
            @PathVariable UUID guestbookId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody GuestBookDto.GuestBookRequest request) {
        return ResponseEntity.ok(
                guestBookService.updateGuestBook(
                        guestbookId, customUserDetails.getMember(), request));
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

    // 즐겨찾기 API
    /**
     * 즐겨찾기에 추가 (구독 추가)
     *
     * @param storeId
     * @param customUserDetails
     * @return
     */
    @Operation(summary = "즐겨찾기 추가하기")
    @PostMapping("/{storeId}/subscribe")
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
    @DeleteMapping("/{storeId}/subscribe")
    public ResponseEntity<Void> unSubscribe(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        subscriptionService.unsubscribe(customUserDetails.getMember().getId(), storeId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 즐겨찾기 목록 조회")
    @GetMapping("/me/favorites")
    public ResponseEntity<Slice<StoreDto.StoreSummaryResponse>> getMyFavorites(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(
                storeService.getMyFavoriteStores(customUserDetails.getMember().getId(), pageable));
    }
}
