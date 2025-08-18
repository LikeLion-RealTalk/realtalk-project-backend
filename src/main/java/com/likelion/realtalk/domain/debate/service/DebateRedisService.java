package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.dto.DebateResultDto;
import com.likelion.realtalk.domain.debate.dto.DebateResultDto.AiSummaryResultDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebateRedisService {

  private final DebateResultService debateResultService;
  private final AiService aiService;
  private final SpeakerService speakerService;
  private final AudienceService audienceService;
  private final DebateRoomService debateRoomService;

  public void endDebate(String roomUUID) {

    debateRoomService.endDebate(roomUUID);
    ArrayList<SpeakerMessageDto> speeches = speakerService.getSpeeches(roomUUID);
    AiSummaryResultDto resultDto = (speeches == null || speeches.isEmpty())
        ? AiSummaryResultDto.empty()
        : aiService.summaryResult(roomUUID, speeches);

    debateResultService.saveDebateResult(roomUUID, resultDto);

    // speak 관련 redis 정보 삭제
    this.speakerService.clearSpeakerCaches(roomUUID);
    // audience 관련 redis 정보 삭제
    this.audienceService.clearAudienceCaches(roomUUID);
    // ai 관련 redis 정보 삭제
    this.aiService.clearAiCaches(roomUUID);
    // TODO. redis 정보 삭제

    // WebSocket 연결 해지 요청
    debateRoomService.pubEndDebate(roomUUID);
  }

}