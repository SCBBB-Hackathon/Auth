package com.example.auth.service;

import com.example.auth.client.social.SocialUserProfile;
import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 구글 프로필을 DB 사용자로 저장하거나 업데이트한다.
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User upsertSocialUser(SocialUserProfile profile) {
        return userRepository
            .findByProviderAndProviderId(profile.provider(), profile.providerId())
            .map(existing -> {
                existing.updateProfile(profile.name(), profile.email());
                return existing;
            })
            .orElseGet(() -> userRepository.save(
                new User(profile.provider(), profile.providerId(), profile.email(), profile.name())
            ));
    }
}
