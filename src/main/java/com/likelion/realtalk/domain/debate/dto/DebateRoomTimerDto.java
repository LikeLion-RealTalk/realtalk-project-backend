package com.likelion.realtalk.domain.debate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class DebateRoomTimerDto {
  private String debateExpireTime;

  @Builder
  public DebateRoomTimerDto(String debateExpireTime) {
    this.debateExpireTime = debateExpireTime;
  }
}