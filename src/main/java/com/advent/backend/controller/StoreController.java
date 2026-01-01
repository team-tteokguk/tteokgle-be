package com.advent.backend.controller;

import com.advent.backend.dto.GuestBookDto;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.dto.StoreDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상점 관리", description = "상점 정보 조회 및 고명 조회, 수정, 등록")
@RestController
@RequestMapping("/stores")
public class StoreController {
    // 상점 API
    // 1. 상점 정보 불러오기
    @Operation(summary = "상점 정보", description = "특정 상점의 정보를 불러옵니다.")
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreDto.StoreResponse> getStoreInfo(@PathVariable String storeId) {
        StoreDto.StoreResponse response =
                StoreDto.StoreResponse.builder().id(storeId).name("외요네 떡국").build();

        return ResponseEntity.ok(response);
    }

    // 2. 고명 리스트 조회하기
    @Operation(summary = "고명 리스트 조회하기")
    @GetMapping("/{storeId}/items")
    public ResponseEntity<List<ItemDto.StoreItemResponse>> getItems(@PathVariable String storeId) {
        ItemDto.StoreItemResponse item1 =
                ItemDto.StoreItemResponse.builder()
                        .id("uuid")
                        .name("계란 지단")
                        .imageUrl(
                                "https://i.namu.wiki/i/lSYFuyRW6FITfZXjJq7cyyqqEYvbMrRx1jvElj09o1XBx2OAUsNZDk9_dOs-5Qx_xPYGF8pRVHTXYd_R4efqEa-vz6kEf5Eq60LICOyLg37p95trbmdLdO7mzl9sG3wAY3OyeDxrzlWB2Ysf8ILzqw.webp")
                        .cost(500)
                        .sellCounts(1)
                        .build();

        ItemDto.StoreItemResponse item2 =
                ItemDto.StoreItemResponse.builder()
                        .id("uuid")
                        .name("애호박 지단")
                        .imageUrl(
                                "https://i.namu.wiki/i/lSYFuyRW6FITfZXjJq7cyyqqEYvbMrRx1jvElj09o1XBx2OAUsNZDk9_dOs-5Qx_xPYGF8pRVHTXYd_R4efqEa-vz6kEf5Eq60LICOyLg37p95trbmdLdO7mzl9sG3wAY3OyeDxrzlWB2Ysf8ILzqw.webp")
                        .cost(400)
                        .sellCounts(3)
                        .build();

        return ResponseEntity.ok(List.of(item1, item2));
    }

    // 3. 고명 등록하기
    // 등록한 고명을 고명 리스트에 바로 업데이트 (고명 리스트 API를 한 번 더 호출하지 않음)
    @Operation(summary = "고명 등록하기")
    @PostMapping("/{storeId}/items")
    public ResponseEntity<ItemDto.StoreItemResponse> createItem(
            @PathVariable String storeId,
            @RequestBody ItemDto.ItemCreateRequest itemCreateRequest) {
        ItemDto.StoreItemResponse item1 =
                ItemDto.StoreItemResponse.builder()
                        .id("uuid")
                        .name(itemCreateRequest.getName())
                        .imageUrl(itemCreateRequest.getImageUrl())
                        .cost(500)
                        .sellCounts(1)
                        .build();

        return ResponseEntity.ok(item1);
    }

    // 4. 고명 삭제하기
    @Operation(summary = "고명 삭제하기")
    @DeleteMapping("/{storeId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable String storeId, @PathVariable String itemId) {
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
            @RequestHeader("Member-Id") String userId,
            @RequestBody GuestBookDto.GuestBookRequest guestBookRequest) {
        GuestBookDto.GuestBookResponse response =
                GuestBookDto.GuestBookResponse.builder()
                        .id("uuid")
                        .writerId(userId)
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
            @RequestHeader("Member-Id") String userId,
            @RequestBody GuestBookDto.GuestBookRequest request) {
        GuestBookDto.GuestBookResponse response =
                GuestBookDto.GuestBookResponse.builder()
                        .id(guestbookId)
                        .writerId(userId)
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
}
