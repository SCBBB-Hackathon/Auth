package com.example.auth.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleOAuthClient {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_ISSUER_ALT = "accounts.google.com";
    private static final String GOOGLE_JWK_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenUri;
    private final JwtDecoder jwtDecoder;

    public GoogleOAuthClient(
        @Value("${google.oauth.client-id}") String clientId,
        @Value("${google.oauth.client-secret}") String clientSecret,
        @Value("${google.oauth.redirect-uri}") String redirectUri,
        @Value("${google.oauth.token-uri:https://oauth2.googleapis.com/token}") String tokenUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.tokenUri = tokenUri;
        this.jwtDecoder = buildDecoder(clientId);
    }

    public GoogleUserProfile exchangeAuthCode(String code, String requestedRedirectUri) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("authorization code is required");
        }
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret) || !StringUtils.hasText(redirectUri)) {
            throw new IllegalStateException("Google OAuth client configuration is missing");
        }
        if (StringUtils.hasText(requestedRedirectUri) && !redirectUri.equals(requestedRedirectUri)) {
            throw new IllegalArgumentException("Redirect URI does not match registered redirect URI");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
            tokenUri,
            new HttpEntity<>(body, headers),
            GoogleTokenResponse.class
        );

        GoogleTokenResponse tokenResponse = response.getBody();
        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.idToken())) {
            throw new IllegalStateException("Failed to exchange code for Google tokens");
        }

        Jwt idToken = jwtDecoder.decode(tokenResponse.idToken());
        return new GoogleUserProfile(
            idToken.getClaimAsString("sub"),
            idToken.getClaimAsString("email"),
            idToken.getClaimAsString("name")
        );
    }

    private JwtDecoder buildDecoder(String audience) {
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_URI).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(),
            new GoogleIssuerValidator(),
            new AudienceValidator(audience)
        );
        nimbus.setJwtValidator(validator);
        return nimbus;
    }

    private record GoogleTokenResponse(
        @JsonProperty("id_token") String idToken
    ) {}

    private static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String audience;

        AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if (!StringUtils.hasText(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            if (token.getAudience() != null && token.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "Google ID token audience does not match client id", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }

    private static class GoogleIssuerValidator implements OAuth2TokenValidator<Jwt> {
        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            String issuer = token.getIssuer() != null ? token.getIssuer().toString() : null;
            if (GOOGLE_ISSUER.equals(issuer) || GOOGLE_ISSUER_ALT.equals(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "Invalid Google issuer", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
