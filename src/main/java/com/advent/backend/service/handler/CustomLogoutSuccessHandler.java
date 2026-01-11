package com.advent.backend.service.handler;

import com.advent.backend.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
    private final RefreshTokenService refreshTokenService;

    @Value("${KAKAO_REST_API_KEY}")
    private String kakaoRestApiKey;

    public CustomLogoutSuccessHandler(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        // 요청 헤더에서 refresh token 추출
        String refreshToken = "";

        // redis에서 refresh token 삭제
        if (refreshToken != null) refreshTokenService.removeRefreshToken(refreshToken);

        String kakaoLogoutUrl =
                "https://kauth.kakao.com/oauth/logout"
                        + "?client_id="
                        + kakaoRestApiKey
                        + "&logout_redirect_uri="
                        + "http://localhost:8080/";

        response.sendRedirect(kakaoLogoutUrl);
    }
}
