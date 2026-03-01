package com.advent.backend.repository;

import com.advent.backend.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByAuthKey(String authKey);

    Optional<RefreshToken> findRefreshTokenByJwtRefreshToken(String jwtRefreshToken);

    void deleteByAuthKey(String authKey);
}
