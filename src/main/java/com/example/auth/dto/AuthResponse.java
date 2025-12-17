package com.example.auth.dto;

// 클라이언트에게 반환하는 액세스/리프레시 토큰 DTO.
public record AuthResponse(String accessToken, String refreshToken) {}
