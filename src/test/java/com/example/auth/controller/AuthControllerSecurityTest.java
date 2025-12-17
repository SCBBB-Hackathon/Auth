package com.example.auth.controller;

import com.example.auth.config.SecurityConfig;
import com.example.auth.dto.UserInfoResponse;
import com.example.auth.security.jwt.JwtAuthenticationFilter;
import com.example.auth.security.jwt.JwtTokenProvider;
import com.example.auth.security.jwt.JwtUserPrincipal;
import com.example.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security를 "켜고" AuthController를 검증하는 Mock 기반 테스트.
 *
 * 목표:
 * - Spring Security 필터체인이 실제로 동작하는지(토큰 없으면 401, 토큰 있으면 principal 주입) 확인
 * - 외부 의존(DB/Redis/Google)은 띄우지 않음
 * - JWT 서명/검증 로직도 Mock(JwtTokenProvider)로 대체
 */
// @WebMvcTest:
// - MVC 슬라이스 테스트(Controller 중심)로, 웹 계층만 가볍게 띄운다.
// - 단, SecurityConfig 같은 추가 설정은 자동으로 올라오지 않으므로 @Import로 포함해야 한다.
@WebMvcTest(controllers = AuthController.class)
// @Import:
// - @WebMvcTest에 "추가로 올릴 빈/설정"을 명시한다.
// - 여기서는 실제 SecurityFilterChain을 구성하기 위해 SecurityConfig와 JwtAuthenticationFilter를 포함한다.
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
// @AutoConfigureMockMvc:
// - MockMvc를 구성한다.
// - 기본값(addFilters=true)으로 Security 필터체인이 실제로 적용되도록 한다.
@AutoConfigureMockMvc
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // @MockBean:
    // - 스프링 컨테이너의 빈을 Mockito Mock으로 대체한다.
    // - AuthController는 이 Mock AuthService를 주입받아 1줄 위임만 수행한다.
    @MockBean
    private AuthService authService;

    // JwtAuthenticationFilter가 주입받는 의존성.
    // 실제 JwtTokenProvider(@Component)는 secret 설정/서명키 생성이 필요하므로,
    // Security 테스트에서는 Mock으로 검증 흐름만 통과시키는 게 가장 간단하다.
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("인증 토큰 없이 /api/auth/me 호출하면 401")
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 필터가 principal을 만들고 /api/auth/me가 200을 반환한다")
    void me_withValidToken_returns200() throws Exception {
        String token = "valid-token";
        JwtUserPrincipal principal = new JwtUserPrincipal(1L, "Jess", "KR", "provider-id");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getPrincipal(token)).willReturn(principal);
        given(authService.me(principal)).willReturn(new UserInfoResponse(1L, "Jess", "KR", "provider-id"));

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.name").value("Jess"))
            .andExpect(jsonPath("$.nationality").value("KR"))
            .andExpect(jsonPath("$.providerId").value("provider-id"));
    }
}

