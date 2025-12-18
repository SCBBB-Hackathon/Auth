package com.example.auth.service;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
// Redis에 리프레시 토큰을 저장하고 회전(RTR) 처리한다.
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";
    private static final String USER_VERSION_PREFIX = "refreshver:";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final Duration refreshTtl;

    public RefreshTokenService(
        StringRedisTemplate redisTemplate,
        UserRepository userRepository,
        @Value("${auth.jwt.refresh-validity-seconds:604800}") long refreshValiditySeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.refreshTtl = Duration.ofSeconds(refreshValiditySeconds);
    }

    public String issue(User user) {
        String token = UUID.randomUUID().toString();
        long version = currentUserTokenVersion(user.getId());
        redisTemplate.opsForValue().set(key(token), value(user.getId(), version), refreshTtl);
        return token;
    }

    public Optional<User> consumeAndRotate(String refreshToken) {
        String key = key(refreshToken);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return Optional.empty();
        }

        ParsedValue parsed = parseValue(stored);
        long currentVersion = currentUserTokenVersion(parsed.userId());
        if (parsed.version() != currentVersion) {
            return Optional.empty();
        }
        redisTemplate.delete(key); // RTR: 이전 토큰 무효화
        return userRepository.findById(parsed.userId());
    }

    /**
     * 로그아웃(전체 세션 폐기): 유저 토큰 버전을 증가시켜 기존 refresh token을 전부 무효화한다.
     * 기존 토큰 키들은 TTL로 자연 만료된다.
     */
    public void revokeAll(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.opsForValue().increment(userVersionKey(userId));
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }

    private String userVersionKey(Long userId) {
        return USER_VERSION_PREFIX + userId;
    }

    private long currentUserTokenVersion(Long userId) {
        String current = redisTemplate.opsForValue().get(userVersionKey(userId));
        if (current == null) {
            return 0L;
        }
        try {
            return Long.parseLong(current);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String value(Long userId, long version) {
        return userId + ":" + version;
    }

    private ParsedValue parseValue(String stored) {
        int idx = stored.indexOf(':');
        if (idx < 0) {
            // 이전 포맷(userId만 저장) 호환: 버전 0으로 처리
            return new ParsedValue(Long.parseLong(stored), 0L);
        }
        long userId = Long.parseLong(stored.substring(0, idx));
        long version = Long.parseLong(stored.substring(idx + 1));
        return new ParsedValue(userId, version);
    }

    private record ParsedValue(long userId, long version) {}
}
