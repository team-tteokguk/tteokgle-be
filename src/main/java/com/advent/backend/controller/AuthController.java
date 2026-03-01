package com.advent.backend.controller;

import com.advent.backend.dto.AuthDto;
import com.advent.backend.dto.TokenDto;
import com.advent.backend.entity.Member;
import com.advent.backend.enums.SocialProvider;
import com.advent.backend.provider.JwtTokenProvider;
import com.advent.backend.repository.RefreshTokenRepository;
import com.advent.backend.service.RefreshTokenService;
import com.advent.backend.service.SocialLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = " 소셜 로그인, 로그아웃, 리프레시 토큰 발급")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SocialLoginService socialLoginService;

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
            @Parameter(description = "소셜 인증 코드", required = true) @RequestParam String code,
            HttpServletResponse response) {

        Member member = socialLoginService.login(provider, code);

        String accessToken = jwtTokenProvider.createAccessToken(member.getId().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId().toString());

        refreshTokenService.saveRefreshToken(member.getId().toString(), refreshToken);

        ResponseCookie refreshTokenCookie =
                ResponseCookie.from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(false) // 프로덕션에서 수정하기
                        .path("/")
                        .maxAge(7 * 24 * 60 * 60)
                        .sameSite("Lax")
                        .build();
        ResponseCookie accessTokenCookie =
                ResponseCookie.from("accessToken", accessToken)
                        .httpOnly(true)
                        .secure(false) // 프로덕션에서 수정하기
                        .path("/")
                        .maxAge(jwtTokenProvider.getAccessTokenValiditySeconds())
                        .sameSite("Lax")
                        .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        boolean isNewMember = member.getCreatedAt().equals(member.getModifiedAt());

        AuthDto.LoginResponse loginResponse =
                AuthDto.LoginResponse.builder()
                        .accessToken(accessToken)
                        .isNewMember(isNewMember)
                        .build();

        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenDto.TokenResponse> refresh(
            HttpServletRequest request, HttpServletResponse response) { // ⭐ HttpServletResponse 추가

        String refreshToken = jwtTokenProvider.resolveRefreshToken(request);
        TokenDto.TokenResponse newToken = refreshTokenService.isRefreshTokenValid(refreshToken);

        // ⭐ 새로운 리프레시 토큰도 발급하는 경우 쿠키 갱신
        if (newToken.getRefreshToken() != null) {
            ResponseCookie refreshTokenCookie =
                    ResponseCookie.from("refreshToken", newToken.getRefreshToken())
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(7 * 24 * 60 * 60)
                            .sameSite("Lax")
                            .build();
            ResponseCookie accessTokenCookie =
                    ResponseCookie.from("accessToken", newToken.getAccessToken())
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(jwtTokenProvider.getAccessTokenValiditySeconds())
                            .sameSite("Lax")
                            .build();

            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

            // body에서는 제거
            // newToken.setRefreshToken(null); // 또는 DTO 수정
        }

        return ResponseEntity.ok(newToken);
    }
}
