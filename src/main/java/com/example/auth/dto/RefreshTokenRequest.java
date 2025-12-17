package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

// 리프레시 토큰으로 재발급 요청 DTO.
public record RefreshTokenRequest(
    @NotBlank String refreshToken
) {}
