package com.advent.backend.provider;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity-in-seconds}")
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInMilliseconds;

    private Key key;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("JWT Secret Key 초기화 실패", e);
            throw new RuntimeException("JWT Secret Key 초기화 실패");
        }
    }

    /** Access Token 생성 */
    public String createAccessToken(String memberId, String role) {
        try {
            Claims claims = Jwts.claims().setSubject(memberId);
            claims.put("role", role);

            Date now = new Date();
            Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Access Token 생성 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    /** Refresh Token 생성 */
    public String createRefreshToken(String memberId) {
        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

            return Jwts.builder()
                    .setSubject(memberId)
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Refresh Token 생성 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    /** 토큰에서 회원 ID 추출 */
    public String getMemberId(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            log.info("만료된 토큰입니다.");
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            log.info("손상된 토큰입니다.");
            throw new BusinessException(ErrorCode.TOKEN_MALFORMED);
        } catch (UnsupportedJwtException e) {
            log.info("지원하지 않는 토큰입니다.");
            throw new BusinessException(ErrorCode.TOKEN_UNSUPPORTED);
        } catch (Exception e) {
            log.info("유효하지 않은 토큰입니다.");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /** 토큰 유효성 검증 */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}
