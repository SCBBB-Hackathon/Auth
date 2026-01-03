package com.example.auth.config;

import com.example.auth.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
// HTTP 보안 설정을 담당한다. JwtAuthenticationFilter로 Bearer 토큰을 읽어들인다.
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${auth.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${auth.cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    @Bean
    // SecurityFilterChain 빈으로 보안 규칙을 정의한다.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // JWT로 인증하므로 CSRF는 비활성화.
            .csrf(csrf -> csrf.disable())
            // 브라우저 로그인 폼/Basic 인증은 사용하지 않는 API.
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // CORS 허용 오리진/헤더/메서드를 명시적으로 설정.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 세션을 사용하지 않는 stateless API 구성.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 인증 실패(토큰 없음/유효하지 않음)는 401로 통일.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                )
            )
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = splitCsv(allowedOrigins);
        if (!origins.isEmpty()) {
            configuration.setAllowedOrigins(origins);
        }
        List<String> originPatterns = splitCsv(allowedOriginPatterns);
        if (!originPatterns.isEmpty()) {
            configuration.setAllowedOriginPatterns(originPatterns);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }
}
