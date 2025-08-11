package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.dto.DebateResultDto;
import com.likelion.realtalk.domain.debate.repository.DebateResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebateRedisService {

  private final DebateResultService debateResultService;
  private final AiService aiService;
  private final SpeakerService speakerService;
  private final AudienceService audienceService;
  private final DebateResultRepository debateResultRepository;

  public DebateResultDto summarySpeeches(String roomUUID) {

    DebateResultDto dto = debateResultService.saveDebateResult(aiService.summaryResult(roomUUID, speakerService.getSpeeches(roomUUID)));

    // speak 관련 redis 정보 삭제
    this.speakerService.clearSpeakerCaches(roomUUID);
    // audience 관련 redis 정보 삭제
    this.audienceService.clearAudienceCaches(roomUUID);
    // ai 관련 redis 정보 삭제
    this.aiService.clearAiCaches(roomUUID);
    // TODO. redis 정보 삭제

    return dto;
  }

}