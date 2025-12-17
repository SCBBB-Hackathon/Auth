package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthRequest(
    // 내부 사용자를 식별하기 위한 PK
    @NotNull Long userId,
    // 외부 provider의 식별자(예: OAuth sub)
    @NotBlank String providerId,
    // 사용자 표시 이름
    @NotBlank String name,
    // 사용자 국적
    @NotBlank String nationality
) {}
