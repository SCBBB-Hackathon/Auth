package com.example.auth.google;

import com.example.auth.social.SocialUserProfile;
import com.example.auth.user.AuthProvider;

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
