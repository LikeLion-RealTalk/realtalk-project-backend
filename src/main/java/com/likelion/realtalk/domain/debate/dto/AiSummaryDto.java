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

  public static AiSummaryDto failure() {
    return AiSummaryDto.builder()
        .summary("AI 요약 중 오류 발생")
        .build();
  }
}