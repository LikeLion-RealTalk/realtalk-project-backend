package com.likelion.realtalk.domain.user.entity;

import com.likelion.realtalk.global.entity.BaseTime;
import jakarta.persistence.*;

@Entity
@Table(name = "user_profile")
public class UserProfile extends BaseTime {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userProfileId;

  @OneToOne
  @JoinColumn(name = "user_id", unique = true, nullable = false)
  private User user;

  private String nickname;
  private String bio;
}
