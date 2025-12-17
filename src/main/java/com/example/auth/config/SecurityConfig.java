package com.example.auth.config;

import com.example.auth.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
// HTTP 보안 설정을 담당한다. JwtAuthenticationFilter로 Bearer 토큰을 읽어들인다.
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    // SecurityFilterChain 빈으로 보안 규칙을 정의한다.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // JWT로 인증하므로 CSRF는 비활성화.
            .csrf(csrf -> csrf.disable())
            // 기본 CORS 설정 사용.
            .cors(Customizer.withDefaults())
            // 세션을 사용하지 않는 stateless API 구성.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // swagger, 토큰 발급, 헬스체크는 무인증으로 허용.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/api/auth/token",
                    "/api/auth/refresh",
                    "/api/auth/google/code",
                    "/actuator/**"
                ).permitAll()
                // 그 외 모든 요청은 인증 필요.
                .anyRequest().authenticated()
            )
            // UsernamePasswordAuthenticationFilter 전에 JWT 필터 삽입.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
