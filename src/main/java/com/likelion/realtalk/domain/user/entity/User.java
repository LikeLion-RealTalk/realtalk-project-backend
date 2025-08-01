package com.likelion.realtalk.domain.user.entity;

import com.likelion.realtalk.domain.auth.entity.Auth;
import com.likelion.realtalk.global.entity.BaseTime;
import jakarta.persistence.*;


@Entity
@Table(name = "user")
public class User extends BaseTime {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String role = "USER";

  private String refreshToken;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private UserProfile userProfile;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private Auth auth;


}
