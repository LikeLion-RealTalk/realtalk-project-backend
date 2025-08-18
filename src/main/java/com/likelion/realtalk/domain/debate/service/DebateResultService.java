package com.likelion.realtalk.domain.debate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.dto.DebateResultDto;
import com.likelion.realtalk.domain.debate.dto.DebateResultDto.AiSummaryResultDto;
import com.likelion.realtalk.domain.debate.entity.DebateResult;
import com.likelion.realtalk.domain.debate.entity.DebateRoom;
import com.likelion.realtalk.domain.debate.repository.DebateResultRepository;
import com.likelion.realtalk.domain.debate.repository.DebateRoomRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebateResultService {

  private final DebateResultRepository debateResultRepository;
  private final DebateRoomRepository debateRoomRepository;
  private final RoomIdMappingService roomIdMappingService;
  private final ObjectMapper mapper;

  @Transactional
  public void saveDebateResult(String roomUUID, AiSummaryResultDto aiSummaryResultDto) {
    // AI 요약 요청
    String aiSummaryJson = "";
    try {
      aiSummaryJson = mapper.writeValueAsString(aiSummaryResultDto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("AI 요약 실패", e);
    }

    // DebateResult 저장
    Long roomId = roomIdMappingService.toPk(UUID.fromString(roomUUID));
    DebateRoom room = debateRoomRepository.findById(roomId).orElseThrow(() ->
      new IllegalStateException("해당 토론방을 찾을 수 없습니다. roomId: " + roomId)
    );
    DebateResult debateResult = DebateResult.builder()
        .debateRoom(room)
        .aiSummary(aiSummaryJson)
        .build();
    debateResultRepository.save(debateResult);
  }

  public DebateResultDto getDebateResult(String roomUUID) {
    Long roomId = roomIdMappingService.toPk(UUID.fromString(roomUUID));
    return debateResultRepository.findDebateresultByDebateRoomId(roomId);
  }
}