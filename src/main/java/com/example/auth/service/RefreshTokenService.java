package com.example.auth.service;

import com.example.auth.user.User;
import com.example.auth.user.UserRepository;
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
        redisTemplate.opsForValue().set(key(token), user.getId().toString(), refreshTtl);
        return token;
    }

    public Optional<User> consumeAndRotate(String refreshToken) {
        String key = key(refreshToken);
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key); // RTR: 이전 토큰 무효화
        return userRepository.findById(Long.parseLong(userId));
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }
}
