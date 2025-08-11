package com.likelion.realtalk.domain.debate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AudienceTimerDto {

  private String audienceExpireTime;

  @Builder
  public AudienceTimerDto(String audienceExpireTime) {
    this.audienceExpireTime = audienceExpireTime;
  }
}