package com.example.auth.google;

// 구글 ID 토큰에서 추출한 최소 사용자 정보.
public record GoogleUserProfile(
    String sub,
    String email,
    String name
) {}
