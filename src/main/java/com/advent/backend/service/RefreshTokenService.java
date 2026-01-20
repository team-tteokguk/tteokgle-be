package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.TokenDto;
import com.advent.backend.entity.RefreshToken;
import com.advent.backend.provider.JwtTokenProvider;
import com.advent.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 리프래시 토큰 저장
    @Transactional
    public void saveRefreshToken(String refreshToken, String authKey) {
        RefreshToken token =
                RefreshToken.builder().jwtRefreshToken(refreshToken).authKey(authKey).build();
        refreshTokenRepository.save(token);
    }

    // 리프레시 토큰 삭제
    @Transactional
    public void removeRefreshToken(String refreshToken) {
        refreshTokenRepository
                .findRefreshTokenByJwtRefreshToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    // 서버(Redis)에 있는 토큰이랑 로컬에서 보낸 토큰이랑 일치하는지 확인
    @Transactional
    public TokenDto.TokenResponse isRefreshTokenValid(String refreshToken) {

        // 1. JwtProvider에서 RT가 만료되었는지 여부 확인
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 헤더에서 가져온 RT에서 id값을 추출한다.
        String memberId = jwtTokenProvider.getMemberId(refreshToken);

        // 3. 레포지토리에서 id값으로 Redis에 저장된 RT를 가져온다.
        RefreshToken savedToken =
                refreshTokenRepository
                        .findByAuthKey(memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFRESHTOKEN_NOT_FOUND));

        // 5. Redis의 RT와 클라이언트의 RT가 일치하는지 확인
        if (!savedToken.getJwtRefreshToken().equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 6. 일치한다면 새 토큰 생성
        TokenDto.TokenResponse newTokens = jwtTokenProvider.createTokenResponse(memberId);

        // 7. Reids에 새 RT 업데이트
        savedToken.update(newTokens.getRefreshToken());
        refreshTokenRepository.save(savedToken);

        return newTokens;
    }
}
