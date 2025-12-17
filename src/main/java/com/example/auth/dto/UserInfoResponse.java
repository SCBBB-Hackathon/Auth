package com.example.auth.dto;

public record UserInfoResponse(
    // 내부 사용자 PK
    Long userId,
    // 사용자 이름
    String name,
    // 사용자 국적
    String nationality,
    // 외부 provider 식별자
    String providerId
) {}
