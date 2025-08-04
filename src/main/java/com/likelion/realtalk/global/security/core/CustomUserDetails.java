package com.likelion.realtalk.global.security.core;

import com.likelion.realtalk.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {
  private final User user;
  private Map<String, Object> attributes;

  public CustomUserDetails(User user) {
    this.user = user;
  }

  public CustomUserDetails(User user, Map<String, Object> attributes) {
    this.user = user;
    this.attributes = attributes;
  }

  // UserDetails 구현부
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
  public Map<String, Object> getAttribute(String name) {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(user.getRole()));
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public Long getUserId() {
    return user.getId();
  }

  @Override
  public String getName() {
    return user.getUsername();
  }
}
