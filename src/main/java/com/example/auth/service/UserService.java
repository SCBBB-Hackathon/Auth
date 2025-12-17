package com.example.auth.service;

import com.example.auth.google.GoogleUserProfile;
import com.example.auth.user.AuthProvider;
import com.example.auth.user.User;
import com.example.auth.user.UserRepository;
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
    public User upsertGoogleUser(GoogleUserProfile profile) {
        return userRepository
            .findByProviderAndProviderId(AuthProvider.GOOGLE, profile.sub())
            .map(existing -> {
                existing.updateProfile(profile.name(), profile.email());
                return existing;
            })
            .orElseGet(() -> userRepository.save(
                new User(AuthProvider.GOOGLE, profile.sub(), profile.email(), profile.name())
            ));
    }
}
