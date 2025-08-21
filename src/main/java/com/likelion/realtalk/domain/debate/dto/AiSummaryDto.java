package com.likelion.realtalk.domain.debate.dto;

import com.likelion.realtalk.domain.debate.type.Side;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSummaryDto {
  public String summary;
  public Long userId;
  public String username;
  public Side side;

  @Builder
  public AiSummaryDto(String summary, Long userId, String username, Side side) {
    this.summary = summary;
    this.userId = userId;
    this.username = username;
    this.side = side;
  }

  public static AiSummaryDto failure() {
    return AiSummaryDto.builder()
        .summary("AI 요약 중 오류 발생")
        .build();
  }
}