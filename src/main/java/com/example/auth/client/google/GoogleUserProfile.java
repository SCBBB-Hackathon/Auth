package com.example.auth.client.google;

import com.example.auth.client.social.SocialUserProfile;
import com.example.auth.entity.AuthProvider;

// 구글 ID 토큰에서 추출한 최소 사용자 정보.
public record GoogleUserProfile(
    String sub,
    String email,
    String name
) implements SocialUserProfile {

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public String providerId() {
        return sub;
    }
}
