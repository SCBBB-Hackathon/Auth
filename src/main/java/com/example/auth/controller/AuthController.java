package com.example.auth.controller;

import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.GoogleAuthCodeRequest;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.dto.UserInfoResponse;
import com.example.auth.security.jwt.JwtUserPrincipal;
import com.example.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
// 인증 관련 엔드포인트를 제공한다.
public class AuthController {

    private final AuthService authService;

    public AuthController(
        AuthService authService
    ) {
        this.authService = authService;
    }

    /**
     * 임시 로그인 엔드포인트: providerId를 받아 JWT 발급.
     * 실제 서비스에서는 OAuth2 로그인/회원 조회 후 principal을 채워야 한다.
     */
    @PostMapping("/token")
    // 클라이언트가 임시로 보낸 사용자 정보를 그대로 JWT로 발급한다.
    public AuthResponse issueToken(@Valid @RequestBody AuthRequest request) {
        return authService.issueToken(request);
    }

    @PostMapping("/google/code")
    // 모바일 서버가 보내는 구글 인가 코드를 받아 우리 JWT로 교환한다.
    public AuthResponse issueTokenFromGoogleCode(@Valid @RequestBody GoogleAuthCodeRequest request) {
        return authService.loginWithGoogleAuthCode(request);
    }

    @PostMapping("/refresh")
    // RTR: 유효한 리프레시 토큰을 소비하고 새 액세스/리프레시 토큰을 발급한다.
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    // 인증된 사용자의 정보를 JWT에서 꺼내 반환한다.
    public UserInfoResponse me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return authService.me(principal);
    }
}
