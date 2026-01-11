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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 헤더에서 토큰 추출
        String token = jwtTokenProvider.resolveToken(request);

        // 토큰이 존재하고 유효한지
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                UUID memberId = jwtTokenProvider.getMemberId(token);

                Member member =
                        memberRepository
                                .findById(memberId)
                                .orElseThrow(
                                        () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

                CustomUserDetails customUserDetails = new CustomUserDetails(member);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                customUserDetails, null, customUserDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } catch (BusinessException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
