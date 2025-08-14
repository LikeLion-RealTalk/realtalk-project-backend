package com.likelion.realtalk.domain.debate.auth;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public class AuthUserPrincipal implements RoomPrincipal {
    private final Long userId;
    private final String userName;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthUserPrincipal(Long userId, String userName,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.userName = userName;
        this.authorities = authorities;
    }

    @Override public boolean isAuthenticated() { return true; }
    @Override public Long getUserId() { return userId; }
    @Override public String getName() { return userName; }
    @Override public String getDisplayName() { return userName; }
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
}
