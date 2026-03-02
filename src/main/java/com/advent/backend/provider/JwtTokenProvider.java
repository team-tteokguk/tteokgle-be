package com.advent.backend.provider;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.TokenDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** */
@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration}") long accessTokenValiditySeconds,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenValiditySeconds) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    /** 토큰 종류에 따라 유효기간을 다르게 설정해 토큰을 생성한다 */
    // 엑세스 토큰
    public String createAccessToken(String memberId) {
        return createToken(memberId, accessTokenValiditySeconds);
    }

    // 리프레시 토큰
    public String createRefreshToken(String memberId) {
        return createToken(memberId, refreshTokenValiditySeconds);
    }

    private String createToken(String subject, long validityInMilliseconds) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public TokenDto.TokenResponse createTokenResponse(String memberId) {
        // 1. 각 토큰 생성 메서드 호출
        String accessToken = createAccessToken(memberId);
        String refreshToken = createRefreshToken(memberId);

        // 2. DTO에 담아서 반환
        return TokenDto.TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenValiditySeconds)
                .build();
    }

    // 토큰에서 내가 사용할 수 있는 RT값을 꺼냄
    public String getMemberId(String token) {
        try {
            String subject =
                    Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload()
                            .getSubject();

            return subject;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVAILD_REFRESH_TOKEN);
        }
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // HTTP Header에서 Access Token 추출
    public String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // EventSource는 Authorization 헤더를 붙이기 어려워 SSE 구독 URL의 쿼리 토큰을 허용합니다.
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.startsWith("/notifications/stream")) {
            String tokenFromQuery = request.getParameter("accessToken");
            if (StringUtils.hasText(tokenFromQuery)) {
                return tokenFromQuery;
            }
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())
                        && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds / 1000;
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        // ⭐ 쿠키에서 먼저 찾기
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    System.out.println(
                            "✅ 쿠키에서 refreshToken 찾음: "
                                    + cookie.getValue().substring(0, 20)
                                    + "...");
                    return cookie.getValue();
                }
            }
        }

        System.out.println("❌ refreshToken을 찾을 수 없음");
        return null;
    }
}
