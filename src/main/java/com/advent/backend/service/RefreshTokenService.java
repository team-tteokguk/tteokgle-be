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

    @Transactional
    public void saveRefreshToken(String refreshToken, String authKey) {
        RefreshToken token =
                RefreshToken.builder().jwtRefreshToken(refreshToken).authKey(authKey).build();
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void removeRefreshToken(String refreshToken) {
        refreshTokenRepository
                .findRefreshTokenByJwtRefreshToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }
}
