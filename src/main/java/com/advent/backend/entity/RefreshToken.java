package com.advent.backend.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@NoArgsConstructor
@RedisHash(value = "refreshToken")
public class RefreshToken {
    @Id private String authKey;

    private String jwtRefreshToken;

    @TimeToLive private Long ttl;

    @Builder
    public RefreshToken(String jwtRefreshToken, String authKey) {
        this.jwtRefreshToken = jwtRefreshToken;
        this.authKey = authKey;
        this.ttl = 1000L * 60 * 60 * 24 * 7;
    }

    public void update(String token) {
        this.jwtRefreshToken = token;
        this.ttl = 1000L * 60 * 60 * 24 * 7;
    }
}
