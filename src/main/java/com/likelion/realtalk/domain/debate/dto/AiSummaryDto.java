package com.likelion.realtalk.domain.debate.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSummaryDto {
  public String summary;
  public Long userId;
  public String username;

  @Builder
  public AiSummaryDto(String summary, Long userId, String username) {
    this.summary = summary;
    this.userId = userId;
    this.username = username;
  }
}