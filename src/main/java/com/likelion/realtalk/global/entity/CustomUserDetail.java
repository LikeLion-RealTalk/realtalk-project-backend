package com.likelion.realtalk.global.entity;

import com.likelion.realtalk.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class CustomUserDetail implements UserDetails, OAuth2User {
  private final User user;
  private final Map<String, Object> attributes;

  public CustomUserDetail(User user, Map<String, Object> attributes) {
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

  // OAuth2User 구현부
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public String getName() {
    return user.getUsername();
  }

  public Long getUserId() {
    return user.getId();
  }

  public String getRole() {
    return user.getRole();
  }
}
