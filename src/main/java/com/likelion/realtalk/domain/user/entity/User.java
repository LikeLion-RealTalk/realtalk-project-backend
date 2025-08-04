package com.likelion.realtalk.domain.user.entity;

import com.likelion.realtalk.domain.auth.entity.Auth;
import com.likelion.realtalk.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseTime {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String role = "USER";

  private String refreshToken;

  @Column(name = "is_username_confirmed", nullable = false)
  private Boolean isUsernameConfirmed = false;


  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private UserProfile userProfile;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private Auth auth;


}
