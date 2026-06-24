package com.linker.relia.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class AuthTokenRepository {
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(String tokenId, String refreshToken, long remainingMillis) {
        stringRedisTemplate.opsForValue().set(buildRefreshKey(tokenId), refreshToken, Duration.ofMillis(remainingMillis));
    }

    public void saveBlacklist(String tokenId, long remainingMillis) {
        stringRedisTemplate.opsForValue().set(buildBlacklistKey(tokenId), "logout", Duration.ofMillis(remainingMillis));
    }

    public boolean hasBlacklistedToken(String tokenId) {
        Boolean hasKey = stringRedisTemplate.hasKey(buildBlacklistKey(tokenId));
        return Boolean.TRUE.equals(hasKey);
    }

    public boolean hasRefreshToken(String tokenId) {
        Boolean hasKey = stringRedisTemplate.hasKey(buildRefreshKey(tokenId));
        return Boolean.TRUE.equals(hasKey);
    }

    public void deleteRefreshToken(String tokenId) {
        stringRedisTemplate.delete(buildRefreshKey(tokenId));
    }

    private String buildBlacklistKey(String tokenId) {
        return BLACKLIST_PREFIX + tokenId;
    }

    private String buildRefreshKey(String tokenId) {
        return REFRESH_PREFIX + tokenId;
    }
}
