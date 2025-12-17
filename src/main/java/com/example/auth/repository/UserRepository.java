package com.example.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.auth.entity.AuthProvider;
import com.example.auth.entity.User;

// 구글 사용자 조회용 레포지토리. provider+providerId로 고유 조회한다.
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
