package com.likelion.realtalk.domain.auth.entity;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auth", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auth extends BaseTime {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long Id;

  @OneToOne
  @JoinColumn(name = "user_id", unique = true, nullable = false)
  private User user;

  private String provider;
  private String providerId;
  private String providerEmail;

}
