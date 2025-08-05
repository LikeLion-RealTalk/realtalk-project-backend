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
public class CustomUserDetails implements UserDetails, OAuth2User {
  private final User user;
  private final Map<String, Object> attributes;

  public CustomUserDetails(User user) {
    this.user = user;
    this.attributes = null;
  }

  public CustomUserDetails(User user, Map<String, Object> attributes) {
    this.user = user;
    this.attributes = attributes;
  }

  // UserDetails 구현
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(user.getRole().getValue()));
  }
  @Override public String getPassword() { return null; }
  @Override public String getUsername() { return user.getUsername(); }
  @Override public boolean isAccountNonExpired() { return true; }
  @Override public boolean isAccountNonLocked() { return true; }
  @Override public boolean isCredentialsNonExpired() { return true; }
  @Override public boolean isEnabled() { return true; }

  // OAuth2User 구현
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }
  @Override
  public Object getAttribute(String name) {
    return attributes != null ? attributes.get(name) : null;
  }
  @Override
  public String getName() {
    return user.getUsername(); // 또는 user.getId().toString() 등 식별자
  }

  // 커스텀 getter
  public Long getUserId() {
    return user.getId();
  }
}
