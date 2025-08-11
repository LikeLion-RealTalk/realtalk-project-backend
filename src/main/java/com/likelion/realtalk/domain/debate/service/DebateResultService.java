package com.likelion.realtalk.domain.debate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.dto.DebateResultDto;
import com.likelion.realtalk.domain.debate.dto.DebateResultDto.AiSummaryResultDto;
import com.likelion.realtalk.domain.debate.entity.DebateResult;
import com.likelion.realtalk.domain.debate.repository.DebateResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebateResultService {

  private final DebateResultRepository debateResultRepository;
  private final ObjectMapper mapper;

  public DebateResultDto saveDebateResult(AiSummaryResultDto aiSummaryResultDto) {
    String aiSummaryJson = "";
    try {
      aiSummaryJson = mapper.writeValueAsString(aiSummaryResultDto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize AiSummaryResultDto", e);
    }

    DebateResult debateResult = DebateResult.builder()
        .aiSummary(aiSummaryJson)
//      .closedAt() // TODO. redis에 저장된 토론 전체 종료 시간을 가져와서 저장
        .build();
    debateResultRepository.save(debateResult);


    // TODO. DebateResultDto 조회해와서 반환 필요
    return null;
  }

}