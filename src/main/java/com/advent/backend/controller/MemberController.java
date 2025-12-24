package com.advent.backend.controller;


import com.advent.backend.dto.MemberDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name= "회원 관리", description = "내 정보 조회, 닉네임 변경, 회원 탈퇴")
@RestController
@RequestMapping("/members")
public class MemberController {

    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 회원의 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<MemberDto.MemberResponse> getMyInfo(
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authorization
    ) {
        MemberDto.MemberResponse response = MemberDto.MemberResponse.builder()
                .userId("user-uuid-123")
                .email("maru@example.com")
                .socialType("KAKAO")
                .nickname("마루")
                .point(1000)
                .createdAt("2025-12-24T00:00:00")
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "닉네임 변경",
            description = "로그인한 회원 자신의 닉네임을 변경합니다."
    )
    @PatchMapping("/me")
    public ResponseEntity<MemberDto.NicknameUpdateRequest> updateNickname(
           @Parameter(hidden = true)
           @RequestHeader("Authorization") String authorization
    ) {
        MemberDto.NicknameUpdateRequest response = MemberDto.NicknameUpdateRequest.builder()
                .nickname("마루팝")
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "기존 정보를 삭제하고 회원탈퇴를 합니다. 이 자업은 되돌릴 수 없습니다."
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember (
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.noContent().build();
    }
}
