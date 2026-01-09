package com.advent.backend.controller;

import com.advent.backend.dto.GuestBookDto;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.dto.StoreDto;
import com.advent.backend.repository.StoreRepository;
import com.advent.backend.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상점 관리", description = "상점 정보 조회 및 고명 조회, 수정, 등록")
@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {
    private final StoreRepository storeRepository;
    private final StoreService storeService;

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
    public ResponseEntity<Page<ItemDto.StoreItemResponse>> getItems(
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
    public ResponseEntity<List<GuestBookDto.GuestBookResponse>> getGuestBookInfo(
            @PathVariable String storeId) {
        GuestBookDto.GuestBookResponse guestbook1 =
                GuestBookDto.GuestBookResponse.builder()
                        .id("uuid")
                        .writerId("wirterid")
                        .writerNickname("외요")
                        .writerImageUrl("외요의이미지")
                        .content("외요야 네 상점 잘 봤어")
                        .createdAt(LocalDateTime.now())
                        .build();

        GuestBookDto.GuestBookResponse guestbook2 =
                GuestBookDto.GuestBookResponse.builder()
                        .id("uuid")
                        .writerId("wirterid")
                        .writerNickname("외요")
                        .writerImageUrl("외요의이미지")
                        .content("외요야 네 상점 잘 봤어")
                        .createdAt(LocalDateTime.now())
                        .build();

        return ResponseEntity.ok(List.of(guestbook1, guestbook2));
    }

    // 2. 방명록 작성하기
    @Operation(summary = "방명록 작성하기")
    @PostMapping("/{storeId}/guestbooks")
    public ResponseEntity<GuestBookDto.GuestBookResponse> createGuestBook(
            @PathVariable String storeId,
            @RequestHeader("Member-Id") String memberId,
            @RequestBody GuestBookDto.GuestBookRequest guestBookRequest) {
        GuestBookDto.GuestBookResponse response =
                GuestBookDto.GuestBookResponse.builder()
                        .id("uuid")
                        .writerId(memberId)
                        .writerNickname("외요")
                        .writerImageUrl("외요 이미지")
                        .content(guestBookRequest.getContent())
                        .createdAt(LocalDateTime.now())
                        .build();

        return ResponseEntity.ok(response);
    }

    // 3. 방명록 수정하기
    @Operation(summary = "방명록 수정하기")
    @PatchMapping("/{storeId}/guestbooks/{guestbookId}")
    public ResponseEntity<GuestBookDto.GuestBookResponse> updateGuestBook(
            @PathVariable String storeId,
            @PathVariable String guestbookId,
            @RequestHeader("Member-Id") String memberId,
            @RequestBody GuestBookDto.GuestBookRequest request) {
        GuestBookDto.GuestBookResponse response =
                GuestBookDto.GuestBookResponse.builder()
                        .id(guestbookId)
                        .writerId(memberId)
                        .writerNickname("외요")
                        .writerImageUrl("외요 이미지")
                        .content(request.getContent())
                        .createdAt(LocalDateTime.now())
                        .build();

        return ResponseEntity.ok(response);
    }

    // 4. 방명록 삭제하기
    @Operation(summary = "방명록 삭제하기")
    @DeleteMapping("/{storeId}/guestbooks/{guestbookId}")
    public ResponseEntity<Void> deleteGuestBook(
            @PathVariable String storeId, @PathVariable String guestbookId) {
        return ResponseEntity.noContent().build();
    }

    // 즐겨찾기
    @Operation(summary = "즐겨찾기 추가하기")
    @PostMapping("/{storeId}/subscription")
    public ResponseEntity<Void> addSubscription(
            @PathVariable UUID storeId, @RequestHeader("Member-Id") UUID memberId) {
        storeService.addSubscription(storeId, memberId);
        return ResponseEntity.noContent().build();
    }
}
