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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath(); // getRequestURI보다 안전
        String method = request.getMethod();

        // ✅ CORS Preflight는 무조건 통과
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // ✅ JWT 검사하면 안 되는 공개 엔드포인트들
        return path.startsWith("/auth/login") // "/auth/login/GOOGLE" 포함
                || path.equals("/auth/refresh")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.equals("/")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = jwtTokenProvider.resolveAccessToken(request);

        // ✅ 토큰 없으면 그냥 다음으로 (permitAll 경로 포함해서 정상 동작)
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰이 있는데 유효하지 않으면 컨텍스트 비우고 통과
        // (여기서 response.sendError(401) 같은 거 하면 공개 경로도 망가짐)
        if (!jwtTokenProvider.validateToken(token)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String memberId = jwtTokenProvider.getMemberId(token);

            Member member =
                    memberRepository
                            .findById(UUID.fromString(memberId))
                            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            CustomUserDetails customUserDetails = new CustomUserDetails(member);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            customUserDetails, null, customUserDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (BusinessException e) {
            log.debug("JWT 인증 실패(BusinessException): {}", e.getErrorCode().getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.debug("JWT 인증 중 예외 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
