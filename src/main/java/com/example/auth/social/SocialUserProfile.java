package com.example.auth.social;

import com.example.auth.user.AuthProvider;

// 외부 소셜 제공자(구글 등)에서 가져온 사용자 정보를 우리 도메인 로직이 소비하기 위한 추상화(포트).
// 도메인/서비스 계층이 특정 제공자 DTO에 직접 의존하지 않도록 한다.
public interface SocialUserProfile {
    AuthProvider provider();
    String providerId();
    String email();
    String name();
}
