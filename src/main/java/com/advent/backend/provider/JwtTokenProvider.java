package com.advent.backend.provider;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.TokenDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
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
        return null;
    }

    // HTTP Header에서 Refreseh Token 추출
    public String resolveRefreshToken(HttpServletRequest request) {
        String token = request.getHeader("Refresh-Token");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }
}
