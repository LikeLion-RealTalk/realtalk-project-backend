package com.likelion.realtalk.domain.debate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SpeakerTimerDto {

  private String speakerExpireTime;
  private String currentUserId;

  @Builder
  public SpeakerTimerDto(String speakerExpireTime, String  currentUserId) {
    this.speakerExpireTime = speakerExpireTime;this.currentUserId = currentUserId;
  }
}