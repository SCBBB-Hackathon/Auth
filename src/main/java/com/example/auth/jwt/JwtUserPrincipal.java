package com.example.auth.jwt;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record JwtUserPrincipal(
    Long userId,
    String name,
    String nationality,
    String providerId
) implements UserDetails {

    private static final Collection<? extends GrantedAuthority> ROLE_USER =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return ROLE_USER;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return providerId;
    }

}
