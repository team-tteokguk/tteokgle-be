package com.advent.backend.service.handler;

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
    @Value("${KAKAO_REST_API_KEY}")
    private String kakaoRestApiKey;

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String kakaoLogoutUrl =
                "https://kauth.kakao.com/oauth/logout"
                        + "?client_id="
                        + kakaoRestApiKey
                        + "&logout_redirect_uri="
                        + "http://localhost:8080/";

        response.sendRedirect(kakaoLogoutUrl);
    }
}
