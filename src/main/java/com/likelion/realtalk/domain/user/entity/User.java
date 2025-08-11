package com.likelion.realtalk.domain.user.entity;

import static com.likelion.realtalk.domain.user.entity.Role.*;

import com.likelion.realtalk.domain.auth.entity.Auth;
import com.likelion.realtalk.domain.common.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTime {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String username;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = USER;

  private String refreshToken;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private UserProfile userProfile;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private Auth auth;

  @Builder
  private User(Long id, String username, Role role, String refreshToken, UserProfile userProfile, Auth auth) {
    this.id = id;
    this.username = username;
    this.role = role;
    this.refreshToken = refreshToken;
    this.userProfile = userProfile;
    this.auth = auth;
  }

  public void updateRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public static User of(String username, Role role) {
    return User.builder()
        .username(username)
        .role(role)
        .build();
  }


}
