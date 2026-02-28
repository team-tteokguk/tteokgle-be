package com.advent.backend.service.handler;

import com.advent.backend.entity.Member;
import com.advent.backend.provider.JwtTokenProvider;
import com.advent.backend.security.CustomUserDetails;
import com.advent.backend.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${KAKAO_REST_API_KEY}")
    private String kakaoRestApiKey;

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        // refresh token 제거
        String refreshToken = jwtTokenProvider.resolveRefreshToken(request);
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenService.removeRefreshToken(refreshToken);
        }

        // access/refresh 쿠키 만료
        expireAuthCookies(response);

        Member.SocialType socialType = extractSocialType(authentication);
        if (socialType == Member.SocialType.KAKAO) {
            String kakaoLogoutUrl =
                    "https://kauth.kakao.com/oauth/logout"
                            + "?client_id="
                            + kakaoRestApiKey
                            + "&logout_redirect_uri="
                            + "http://localhost:8080/";

            response.sendRedirect(kakaoLogoutUrl);
            return;
        }

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    private Member.SocialType extractSocialType(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getMember().getSocialType();
        }
        return null;
    }

    private void expireAuthCookies(HttpServletResponse response) {
        ResponseCookie refreshTokenCookie =
                ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(0)
                        .sameSite("Lax")
                        .build();
        ResponseCookie accessTokenCookie =
                ResponseCookie.from("accessToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(0)
                        .sameSite("Lax")
                        .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
    }
}
