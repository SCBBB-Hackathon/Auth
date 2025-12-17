package com.example.auth.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 구글 사용자 조회용 레포지토리. provider+providerId로 고유 조회한다.
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
