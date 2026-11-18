package com.sellerintelligence.core.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Representação do principal autenticado no Spring Security.
 */
public class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final String email;
    private final UUID activeTenantId;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(UUID userId, String email, UUID activeTenantId, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.activeTenantId = activeTenantId;
        this.authorities = roles != null
                ? roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toUnmodifiableList())
                : Collections.emptyList();
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getActiveTenantId() {
        return activeTenantId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
