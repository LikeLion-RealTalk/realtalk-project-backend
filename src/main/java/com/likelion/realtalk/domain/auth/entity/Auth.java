package com.likelion.realtalk.domain.auth.entity;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.common.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auth extends BaseTime {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long Id;

  @OneToOne
  @JoinColumn(name = "user_id", unique = true, nullable = false)
  private User user;

  private String provider;
  private String providerId;
  private String providerEmail;

  @Builder
  private Auth(Long id, User user, String provider, String providerId, String providerEmail) {
    this.Id = id;
    this.user = user;
    this.provider = provider;
    this.providerId = providerId;
    this.providerEmail = providerEmail;
  }

  public static Auth of(User user, String provider, String providerId, String providerEmail) {
    return Auth.builder()
        .user(user)
        .provider(provider)
        .providerId(providerId)
        .providerEmail(providerEmail)
        .build();
  }

}
