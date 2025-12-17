package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthCodeRequest(
    @NotBlank String code,
    String redirectUri
) {}
