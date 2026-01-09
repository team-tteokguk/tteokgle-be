package com.advent.backend.controller;

import com.advent.backend.dto.MemberDto;
import com.advent.backend.entity.Member;
import com.advent.backend.security.CustomUserDetails;
import com.advent.backend.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 관리", description = "내 정보 조회, 닉네임 변경, 회원 탈퇴")
@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "닉네임 유효성 검사", description = "회원 가입 전 닉네임 생성을 위해 닉네임 유효성 검사를 진행합니다.")
    @PostMapping("/nickname")
    public ResponseEntity<Boolean> isNicknameValid(@RequestParam("nickname") String newNickname) {
        memberService.validateNickname(newNickname);
        return ResponseEntity.ok(true);
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<MemberDto.MemberResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Member member = customUserDetails.getMember();
        return ResponseEntity.ok(MemberDto.MemberResponse.from(member));
    }

    @Operation(summary = "닉네임 변경", description = "로그인한 회원 자신의 닉네임을 변경합니다.")
    @PatchMapping("/me")
    public ResponseEntity<Void> updateNickname(
            @RequestParam("nickname") String newNickname,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        memberService.updateNickName(customUserDetails.getMember().getId(), newNickname);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴", description = "기존 정보를 삭제하고 회원탈퇴를 합니다. 이 자업은 되돌릴 수 없습니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        memberService.deleteMember(customUserDetails.getMember().getId());
        return ResponseEntity.noContent().build();
    }
}
