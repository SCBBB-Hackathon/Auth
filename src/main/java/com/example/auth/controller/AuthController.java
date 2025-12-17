package com.example.auth.controller;

import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.GoogleAuthCodeRequest;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.dto.UserInfoResponse;
import com.example.auth.google.GoogleOAuthClient;
import com.example.auth.google.GoogleUserProfile;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.jwt.JwtUserPrincipal;
import com.example.auth.service.RefreshTokenService;
import com.example.auth.user.User;
import com.example.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
// 인증 관련 엔드포인트를 제공한다.
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuthClient googleOAuthClient;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
        JwtTokenProvider jwtTokenProvider,
        GoogleOAuthClient googleOAuthClient,
        UserService userService,
        RefreshTokenService refreshTokenService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleOAuthClient = googleOAuthClient;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 임시 로그인 엔드포인트: providerId를 받아 JWT 발급.
     * 실제 서비스에서는 OAuth2 로그인/회원 조회 후 principal을 채워야 한다.
     */
    @PostMapping("/token")
    // 클라이언트가 임시로 보낸 사용자 정보를 그대로 JWT로 발급한다.
    public AuthResponse issueToken(@Valid @RequestBody AuthRequest request) {
        JwtUserPrincipal principal = new JwtUserPrincipal(
            request.userId(),
            request.name(),
            request.nationality(),
            request.providerId()
        );
        String token = jwtTokenProvider.createAccessToken(principal);
        return new AuthResponse(token, null);
    }

    @PostMapping("/google/code")
    // 모바일 서버가 보내는 구글 인가 코드를 받아 우리 JWT로 교환한다.
    public AuthResponse issueTokenFromGoogleCode(@Valid @RequestBody GoogleAuthCodeRequest request) {
        GoogleUserProfile profile = googleOAuthClient.exchangeAuthCode(request.code(), request.redirectUri());
        User user = userService.upsertSocialUser(profile);
        JwtUserPrincipal principal = principalOf(user);
        String token = jwtTokenProvider.createAccessToken(principal);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(token, refreshToken);
    }

    @PostMapping("/refresh")
    // RTR: 유효한 리프레시 토큰을 소비하고 새 액세스/리프레시 토큰을 발급한다.
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        User user = refreshTokenService.consumeAndRotate(request.refreshToken())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        JwtUserPrincipal principal = principalOf(user);
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken);
    }

    @GetMapping("/me")
    // 인증된 사용자의 정보를 JWT에서 꺼내 반환한다.
    public UserInfoResponse me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        return new UserInfoResponse(principal.userId(), principal.name(), principal.nationality(), principal.providerId());
    }

    private JwtUserPrincipal principalOf(User user) {
        return new JwtUserPrincipal(
            user.getId(),
            user.getName() != null ? user.getName() : user.getEmail(),
            null,
            user.getProviderId()
        );
    }
}
