package com.advent.backend.service.handler;

import com.advent.backend.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String nickname = customUserDetails.getMember().getNickname();

        // 닉네임 존재 여부 확인
        String targetUrl;
        if (nickname == null) { // 닉네임이 없다면 닉네임 만드는 화면으로 리다이렉션
            targetUrl = "/swagger-ui/index.html";
        } else { // 닉네임이 있다면 메인 홈페이지 (현재는 임시 홈페이지)
            targetUrl = "/swagger-ui/index.html";
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
