package com.example.auth.service;

import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.GoogleAuthCodeRequest;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.dto.UserInfoResponse;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.security.jwt.JwtUserPrincipal;
import com.example.auth.entity.User;
import com.example.auth.client.google.GoogleOAuthClient;
import com.example.auth.client.google.GoogleUserProfile;
import com.example.auth.security.jwt.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuthClient googleOAuthClient;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
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

    public AuthResponse issueToken(AuthRequest request) {
        JwtUserPrincipal principal = new JwtUserPrincipal(
            request.userId(),
            request.name(),
            request.nationality(),
            request.providerId()
        );
        String token = jwtTokenProvider.createAccessToken(principal);
        return new AuthResponse(token, null);
    }

    public AuthResponse loginWithGoogleAuthCode(GoogleAuthCodeRequest request) {
        GoogleUserProfile profile = googleOAuthClient.exchangeAuthCode(request.code(), request.redirectUri());
        User user = userService.upsertSocialUser(profile);
        JwtUserPrincipal principal = principalOf(user);
        String token = jwtTokenProvider.createAccessToken(principal);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(token, refreshToken);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        User user = refreshTokenService.consumeAndRotate(request.refreshToken())
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        JwtUserPrincipal principal = principalOf(user);
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken);
    }

    public UserInfoResponse me(JwtUserPrincipal principal) {
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
