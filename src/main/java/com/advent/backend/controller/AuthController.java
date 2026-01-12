package com.advent.backend.controller;

import com.advent.backend.dto.AuthDto;
import com.advent.backend.enums.SocialProvider;
import com.advent.backend.provider.JwtTokenProvider;
import com.advent.backend.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = " 소셜 로그인, 로그아웃, 리프레시 토큰 발급")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // accessToken & refreshToken 관리 방식 상의하기
    @Operation(
            summary = "소셜 로그인",
            description = "카카오 또는 구글 소셜 로그인을 처리합니다. 신규 사용자의 경우 자동으로 회원가입됩니다.")
    @PostMapping("/login/{provider}")
    public ResponseEntity<AuthDto.LoginResponse> socialLogin(
            @Parameter(
                            description = "소셜 로그인 제공자 (kakao, google)",
                            required = true,
                            example = "kakao")
                    @PathVariable
                    SocialProvider provider,
            @Parameter(description = "소셜 인증 코드", required = true) @RequestParam String code) {
        AuthDto.LoginResponse response =
                AuthDto.LoginResponse.builder()
                        .accessToken("sample-access-token")
                        .refreshToken("sample-refresh-token")
                        .isNewMember(false)
                        .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "현재 사용자를 로그아웃 처리합니다. 리프레시 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String refreshToken = jwtTokenProvider.resolveRefreshToken(request);

        // 토큰 유효성 검사
        if (refreshToken == null && jwtTokenProvider.validateToken(refreshToken)) {
            UUID memberId = jwtTokenProvider.getMemberId(refreshToken);

            if (refreshTokenService.isRefreshTokenValid(memberId.toString(), refreshToken)) {
                String newAccessToken = jwtTokenProvider.createAccessToken(memberId.toString());
                String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId.toString());

                refreshTokenService.removeRefreshToken(refreshToken);
                refreshTokenService.saveRefreshToken(newRefreshToken, memberId.toString());

                return ResponseEntity.ok()
                        .header("Authorization", "Bearer " + newAccessToken)
                        .header("Refresh-Token", "Bearer " + newRefreshToken)
                        .body("Token reissued successfully");
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Refreshed Token");
    }
}
