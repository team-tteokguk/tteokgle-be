package com.advent.backend.config;

import com.advent.backend.service.handler.CustomLogoutSuccessHandler;
import com.advent.backend.service.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    public static final String[] ALLOWED_URLS = {
        "/",
        "/error",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/api/v1/posts/**",
        "/api/v1/replies/**",
        "/nickname",
        "/login", // 커스텀 로그인 페이지
        "/auth/login/kakao/**" // 사용자 정의 경로
    };

    private final OAuth2SuccessHandler successHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(ALLOWED_URLS)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2Login(oauth2 -> oauth2.successHandler(successHandler))
                .logout(
                        logout ->
                                logout.logoutUrl("/logout") // 로그아웃을 처리할 엔드포인트 (기본값: /logout)
                                        .logoutSuccessHandler(customLogoutSuccessHandler)
                                        .invalidateHttpSession(true) // 서버 세션 삭제
                                        .clearAuthentication(true) // 권한 정보 삭제
                                        .deleteCookies("JSESSIONID") // 브라우저 쿠키 삭제
                        );

        return http.build();
    }
}
