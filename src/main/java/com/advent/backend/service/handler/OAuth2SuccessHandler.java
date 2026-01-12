package com.advent.backend.service.handler;

import com.advent.backend.provider.JwtTokenProvider;
import com.advent.backend.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String memberId = customUserDetails.getMember().getId().toString();
        String nickname = customUserDetails.getMember().getNickname();

        String accessToken = jwtTokenProvider.createAccessToken(memberId, "ROLE_USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

        String baseUrl = "http://localhost:8080";
        // 닉네임 존재 여부 확인
        String targetPath;

        if (nickname == null) { // 닉네임이 없다면 닉네임 만드는 화면으로 리다이렉션
            targetPath = "/swagger-ui/index.html";
        } else { // 닉네임이 있다면 메인 홈페이지 (현재는 임시 홈페이지)
            targetPath = "/swagger-ui/index.html";
        }

        // URL 뒤에 토큰 붙이기
        String targetUrl =
                UriComponentsBuilder.fromUriString(baseUrl + targetPath)
                        .queryParam("accessToken", accessToken)
                        .queryParam("refreshToken", refreshToken)
                        .queryParam("isNewMember", nickname == null)
                        .build()
                        .toUriString();

        log.info("Redirect URL: {}", targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
