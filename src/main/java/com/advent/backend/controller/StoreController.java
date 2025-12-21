package com.advent.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "상점 관리", description = "상점 정보 조회 및 고명 조회, 수정, 등록")
@RestController
@RequestMapping("/stores")
public class StoreController {
    // 상점 API
    // 1. 상점 정보 불러오기
    @Operation(summary = "상점 정보", description = "특정 상점의 정보를 불러옵니다.")
    @GetMapping("/{storeId}")
    public ResponseEntity<?> getStoreInfo(@PathVariable String storeId){
        return ResponseEntity.ok(Map.of(
                "storeId", storeId,
                "name", "행복한 고명 상점",
                "owner", "외요"
        ));
    }
    // 2. 고명 리스트 조회하기
    @Operation(summary = "고명 리스트 조회하기")
    @GetMapping("/{storeId}/items")
    public ResponseEntity<?> addItem(
            @PathVariable String storeId,
            @RequestBody String item){
        return ResponseEntity.ok(Map.of());
    }
    // 3. 고명 등록하기

    // 4. 고명 삭제하기

    // 방명록 API
    // 1. 방명록 불러오기

    // 2. 방명록 작성하기

    // 3. 방명록 수정하기

    // 4. 방명록 삭제하기
}
