package com.example.auth.service;

import com.example.auth.client.google.GoogleOAuthClient;
import com.example.auth.client.google.GoogleUserProfile;
import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.GoogleAuthCodeRequest;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.dto.UserInfoResponse;
import com.example.auth.entity.AuthProvider;
import com.example.auth.entity.User;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.security.jwt.JwtTokenProvider;
import com.example.auth.security.jwt.JwtUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * "순수 JUnit + Mockito" 단위 테스트 예시:
 * - Spring 컨테이너를 띄우지 않고, @Mock/@InjectMocks로 대상 클래스만 테스트한다.
 * - 외부 의존(DB/Redis/Google API/JWT 서명 등)은 전부 Mock으로 대체한다.
 */
// @ExtendWith(MockitoExtension.class):
// - JUnit5가 Mockito 기능(@Mock, @InjectMocks 등)을 사용할 수 있게 확장 등록한다.
// - 이게 없으면 @Mock이 자동 초기화되지 않는다.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock:
    // - Mockito가 가짜 객체(Mock)를 만든다.
    // - 실제 구현을 실행하지 않고, 우리가 지정한(stub) 동작만 수행한다.
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    // @InjectMocks:
    // - 테스트 대상 클래스(AuthService)를 생성하면서, 위의 @Mock 필드를 생성자/필드에 주입한다.
    // - 스프링 @Autowired와 비슷한 역할을 Mockito가 대신 해준다.
    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("issueToken: 요청 정보를 principal로 만들고 JwtTokenProvider 결과를 반환한다")
    void issueToken() {
        given(jwtTokenProvider.createAccessToken(any(JwtUserPrincipal.class)))
            .willReturn("access-token");

        AuthRequest request = new AuthRequest(1L, "provider-id", "name", "KR");

        AuthResponse response = authService.issueToken(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNull();

        ArgumentCaptor<JwtUserPrincipal> captor = ArgumentCaptor.forClass(JwtUserPrincipal.class);
        then(jwtTokenProvider).should().createAccessToken(captor.capture());

        JwtUserPrincipal used = captor.getValue();
        assertThat(used.userId()).isEqualTo(1L);
        assertThat(used.providerId()).isEqualTo("provider-id");
        assertThat(used.name()).isEqualTo("name");
        assertThat(used.nationality()).isEqualTo("KR");
    }

    @Test
    @DisplayName("loginWithGoogleAuthCode: 구글 프로필 교환→유저 upsert→토큰 발급까지 수행한다")
    void loginWithGoogleAuthCode() {
        given(googleOAuthClient.exchangeAuthCode("auth-code", "app://redirect"))
            .willReturn(new GoogleUserProfile("sub-123", "user@example.com", "Jess"));

        given(userService.upsertSocialUser(any()))
            .willReturn(new User(AuthProvider.GOOGLE, "sub-123", "user@example.com", "Jess"));

        given(jwtTokenProvider.createAccessToken(any(JwtUserPrincipal.class)))
            .willReturn("access-token");

        given(refreshTokenService.issue(any(User.class)))
            .willReturn("refresh-token");

        AuthResponse response = authService.loginWithGoogleAuthCode(
            new GoogleAuthCodeRequest("auth-code", "app://redirect")
        );

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("refresh: 리프레시 토큰이 유효하지 않으면 UnauthorizedException")
    void refresh_invalidToken() {
        given(refreshTokenService.consumeAndRotate("bad-refresh"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("bad-refresh")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("me: principal이 null이면 null을 반환한다")
    void me_nullPrincipal() {
        UserInfoResponse response = authService.me(null);
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("logout: 유저 토큰 버전을 올려 모든 세션을 무효화한다")
    void logout_revokesAllSessions() {
        JwtUserPrincipal principal = new JwtUserPrincipal(1L, "Jess", "KR", "provider-id");

        authService.logout(principal);

        then(refreshTokenService).should(times(1)).revokeAll(1L);
    }

    @Test
    @DisplayName("logout: principal 또는 userId가 없으면 UnauthorizedException")
    void logout_requiresAuthenticatedUser() {
        assertThatThrownBy(() -> authService.logout(null))
            .isInstanceOf(UnauthorizedException.class);

        JwtUserPrincipal noUserId = new JwtUserPrincipal(null, "Jess", "KR", "provider-id");
        assertThatThrownBy(() -> authService.logout(noUserId))
            .isInstanceOf(UnauthorizedException.class);
    }
}
