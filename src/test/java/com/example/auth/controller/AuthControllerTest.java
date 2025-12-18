package com.example.auth.controller;

import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.GoogleAuthCodeRequest;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.security.jwt.JwtTokenProvider;
import com.example.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mock 기반 테스트 예시:
 * - 컨트롤러가 "서비스에 위임만" 잘 하는지 검증한다.
 * - 외부 의존(MySQL/Redis/Google API/JWT 등)은 전혀 띄우지 않는다.
 */
// @WebMvcTest:
// - Spring Boot 테스트 중 "웹 MVC 계층"만 슬라이스로 띄운다.
// - 기본적으로 Controller, Jackson(ObjectMapper), Validation 등 웹에 필요한 빈만 로드한다.
// - Service/Repository 같은 빈은 자동으로 로드되지 않으므로 @MockBean으로 대체한다.
// excludeAutoConfiguration:
// - Spring Security 자동설정을 끄면(SecurityAutoConfiguration/SecurityFilterAutoConfiguration),
//   테스트 컨텍스트에서 보안 필터/체인 빈 생성이 생략되어 인증 구성에 의존하지 않게 된다.
@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
// @Import:
// - @WebMvcTest 슬라이스는 "필요한 것만" 올리므로, 전역 예외 처리(@RestControllerAdvice)를 명시적으로 포함시킨다.
@Import(GlobalExceptionHandler.class)
// @AutoConfigureMockMvc:
// - MockMvc(가짜 HTTP 클라이언트)를 자동 구성한다.
// - addFilters=false로 Spring Security 필터체인을 끄면 인증/인가에 막히지 않고 컨트롤러 동작만 테스트할 수 있다.
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    // @Autowired:
    // - 스프링 컨테이너가 만들어둔 빈을 주입받는다.
    @Autowired
    private MockMvc mockMvc;

    // @Autowired:
    // - 요청/응답 바디 JSON 직렬화/역직렬화에 사용할 ObjectMapper를 주입받는다.
    @Autowired
    private ObjectMapper objectMapper;

    // @MockBean:
    // - 스프링 컨테이너에 "AuthService 빈"을 Mockito Mock으로 등록한다.
    // - 컨트롤러는 이 Mock을 주입받아 호출하게 되고, 우리는 호출 결과를 원하는대로 stub 할 수 있다.
    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // @Test:
    // - JUnit5 테스트 메서드임을 표시한다.
    @Test
    // @DisplayName:
    // - 테스트 리포트에 보여줄 이름(한글 가능)을 지정한다.
    @DisplayName("POST /api/auth/token 은 AuthService.issueToken 결과를 그대로 반환한다")
    void issueToken_delegates_to_service() throws Exception {
        given(authService.issueToken(any(AuthRequest.class)))
            .willReturn(new AuthResponse("access-token", null));

        AuthRequest request = new AuthRequest(1L, "provider-id", "name", "KR");

        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value(nullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/google/code 는 AuthService.loginWithGoogleAuthCode 결과를 그대로 반환한다")
    void googleLogin_delegates_to_service() throws Exception {
        given(authService.loginWithGoogleAuthCode(any(GoogleAuthCodeRequest.class)))
            .willReturn(new AuthResponse("access-token", "refresh-token"));

        GoogleAuthCodeRequest request = new GoogleAuthCodeRequest("auth-code", "app://redirect");

        mockMvc.perform(post("/api/auth/google/code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh 가 UnauthorizedException 을 받으면 401을 반환한다")
    void refresh_returns_401_when_service_throws_unauthorized() throws Exception {
        willThrow(new UnauthorizedException("Invalid refresh token"))
            .given(authService)
            .refresh(any(RefreshTokenRequest.class));

        RefreshTokenRequest request = new RefreshTokenRequest("bad-refresh-token");

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid refresh token"));
    }

    @Test
    @DisplayName("GET /api/auth/me 는 AuthService.me 결과를 그대로 반환한다(인증 Principal 없이도 위임은 가능)")
    void me_delegates_to_service() throws Exception {
        given(authService.me(null))
            .willReturn(null);

        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isOk());
    }
}
