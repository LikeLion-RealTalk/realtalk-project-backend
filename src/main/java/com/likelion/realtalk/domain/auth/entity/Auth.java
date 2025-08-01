package com.likelion.realtalk.domain.auth.entity;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.global.entity.BaseTime;
import jakarta.persistence.*;

@Entity
@Table(name = "auth", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerId"}))
public class Auth extends BaseTime {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long authId;

  @OneToOne
  @JoinColumn(name = "user_id", unique = true, nullable = false)
  private User user;

  private String provider;
  private String providerId;
  private String providerEmail;

}
