package com.advent.backend.config;

import com.advent.backend.config.filter.JwtAuthenticationFilter;
import com.advent.backend.provider.JwtTokenProvider;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.service.handler.CustomLogoutSuccessHandler;
import com.advent.backend.service.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
        "/auth/login/kakao/**", // 사용자 정의 경로
        "/auth/login/GOOGLE/**",
        "/auth/login/**",
        "/auth/refresh", // AT 재발급
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final OAuth2SuccessHandler successHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
                .csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/auth/**")
                                        .permitAll()
                                        .requestMatchers(ALLOWED_URLS)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2Login(
                        oauth2 ->
                                oauth2.successHandler(successHandler)
                                        .failureHandler(
                                                (request, response, exception) -> {
                                                    // 여기서 진짜 에러 메시지를 콘솔에 찍습니다.
                                                    System.out.println(
                                                            "❌ OAuth2 로그인 실패 원인: "
                                                                    + exception.getMessage());
                                                    exception.printStackTrace();
                                                    response.sendRedirect(
                                                            "/swagger-ui/index.html?error="
                                                                    + exception.getMessage());
                                                }))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, memberRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        exception ->
                                exception.authenticationEntryPoint(
                                        new HttpStatusEntryPoint(
                                                HttpStatus.UNAUTHORIZED)) // 302 대신 401 응답
                        )
                .logout(
                        logout ->
                                logout.logoutUrl("/logout") // 로그아웃을 처리할 엔드포인트 (기본값: /logout)
                                        .logoutSuccessHandler(customLogoutSuccessHandler));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*"); // 모든 Origin 허용 (테스트용)
        configuration.addAllowedMethod("*"); // 모든 HTTP Method 허용
        configuration.addAllowedHeader("*"); // 모든 Header 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
