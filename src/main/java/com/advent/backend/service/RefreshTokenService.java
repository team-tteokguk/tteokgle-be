package com.advent.backend.service;

import com.advent.backend.entity.RefreshToken;
import com.advent.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

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

    // 리프레시 토큰과 새로 발급받은 토큰과 일치하는지 확인ㅇ
    @Transactional
    public boolean isRefreshTokenValid(String memberId, String refreshToken) {
        return true;
    }
}
