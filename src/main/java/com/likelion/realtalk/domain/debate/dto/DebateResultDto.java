package com.likelion.realtalk.domain.debate.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.type.DebateType;
import com.likelion.realtalk.global.exception.DataRetrievalException;
import com.likelion.realtalk.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
public class DebateResultDto {

  private DebateType debateType; // 토론 유형
  private String title; // 토론 제목
  private String categoryName; // 토론 카테고리
  private double sideARate; // A측 비율
  private double sideBCRate; // B측 비율
  private Long totalCount; // 전체 참여자 수
  private String sideA; // A측 이름
  private String sideB; // B측 이름
  private Long durationSeconds;
  private AiSummaryResultDto aiSummaryResult; // ai 요약 결과

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AiSummaryResultDto {
    private static final String EMPTY_MSG = "발언 내용이 없습니다.";
    private String sideA; // A측 요약 결과
    private String sideB; // B측 요약 결과
    private String aiResult; // 전체 요약 결과

    public static AiSummaryResultDto empty() {
      return new AiSummaryResultDto(EMPTY_MSG, EMPTY_MSG, EMPTY_MSG);
    }
  }

  public DebateResultDto(DebateType debateType, String title, String categoryName, double sideARate,
      double sideBCRate, Long totalCount, String sideA, String sideB, String aiSummaryResult,
      Long durationSeconds) {
    ObjectMapper objectMapper = new ObjectMapper();
    this.debateType = debateType;
    this.title = title;
    this.categoryName = categoryName;
    this.sideARate = sideARate;
    this.sideBCRate = sideBCRate;
    this.totalCount = totalCount;
    this.sideA = sideA;
    this.sideB = sideB;
    this.durationSeconds = durationSeconds;
    try {
      this.aiSummaryResult = objectMapper.readValue(aiSummaryResult, AiSummaryResultDto.class);
    } catch (JsonProcessingException e) {
      throw new DataRetrievalException(ErrorCode.JSON_PROCESSING_ERROR);
    }
  }
}
