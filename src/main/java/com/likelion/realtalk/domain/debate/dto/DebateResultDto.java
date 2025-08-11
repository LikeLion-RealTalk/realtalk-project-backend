package com.likelion.realtalk.domain.debate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class DebateResultDto {

  private String debateType;
  private String title;
  private String categoryName;
  private float sideARate;
  private float sideBRate;
  private AiSummaryResultDto aiSummaryResult;

  @Getter
  public class AiSummaryResultDto {
    private String sideA;
    private String sideB;
    private String aiResult;

    public AiSummaryResultDto(String sideA, String sideB, String aiResult) {
      this.sideA = sideA;
      this.sideB = sideB;
      this.aiResult = aiResult;
    }
  }

  @Builder
  public DebateResultDto(String debateType, String title, String categoryName, float sideARate,
      float sideBRate, AiSummaryResultDto aiSummaryResult) {
    this.debateType = debateType;
    this.title = title;
    this.categoryName = categoryName;
    this.sideARate = sideARate;
    this.sideBRate = sideBRate;
    this.aiSummaryResult = aiSummaryResult;
  }
}
