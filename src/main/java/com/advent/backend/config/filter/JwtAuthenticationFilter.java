package com.advent.backend.config.filter;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.Member;
import com.advent.backend.provider.JwtTokenProvider;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    private static final List<String> EXCLUDED_PATHS =
            Arrays.asList("/auth/login/", "/auth/refresh", "/swagger-ui/", "/error");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 헤더에서 토큰 추출
        String token = jwtTokenProvider.resolveAccessToken(request);

        // 토큰이 존재하고 유효한지
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                String memberId = jwtTokenProvider.getMemberId(token);

                Member member =
                        memberRepository
                                .findById(UUID.fromString(memberId))
                                .orElseThrow(
                                        () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

                CustomUserDetails customUserDetails = new CustomUserDetails(member);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                customUserDetails, null, customUserDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } catch (BusinessException e) {
                log.debug("JWT 인증 실패: {}", e.getErrorCode().getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                log.debug("JWT 인증 중 예외 발생: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
