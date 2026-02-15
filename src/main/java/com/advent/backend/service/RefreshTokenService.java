package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.TokenDto;
import com.advent.backend.entity.RefreshToken;
import com.advent.backend.provider.JwtTokenProvider;
import com.advent.backend.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    // 리프래시 토큰 저장
    @Transactional
    public void saveRefreshToken(String authKey, String refreshToken) {
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
        // 1. RT가 없는 경우
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 2. JwtProvider에서 RT가 만료되었는지 여부 확인
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        // 2. 헤더에서 가져온 RT에서 id값을 추출한다.
        String memberId = jwtTokenProvider.getMemberId(refreshToken);

        // 3. 레포지토리에서 id값으로 Redis에 저장된 RT를 가져온다.
        RefreshToken savedToken =
                refreshTokenRepository
                        .findByAuthKey(memberId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 5. Redis의 RT와 클라이언트의 RT가 일치하는지 확인
        if (!savedToken.getJwtRefreshToken().equals(refreshToken)) {
            // TODO: 보안상 로그까지 남기는 것이 좋음
            throw new BusinessException(ErrorCode.INVAILD_REFRESH_TOKEN);
        }

        // 회원 데이터가 사라진 경우(예: 개발환경 DB 재생성) refresh 성공처럼 보이더라도
        // 이후 모든 요청이 401이 되므로 여기서 명시적으로 차단한다.
        if (!memberRepository.existsById(java.util.UUID.fromString(memberId))) {
            refreshTokenRepository.delete(savedToken);
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 6. 일치한다면 새 토큰 생성
        TokenDto.TokenResponse newTokens = jwtTokenProvider.createTokenResponse(memberId);

        // 7. Reids에 새 RT 업데이트
        savedToken.update(newTokens.getRefreshToken());
        refreshTokenRepository.save(savedToken);

        return newTokens;
    }
}
