package com.likelion.realtalk.domain.user.entity;

import static com.likelion.realtalk.domain.user.entity.Role.*;

import com.likelion.realtalk.domain.auth.entity.Auth;
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


}
