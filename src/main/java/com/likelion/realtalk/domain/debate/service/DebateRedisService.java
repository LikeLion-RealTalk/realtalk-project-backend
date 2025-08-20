package com.likelion.realtalk.domain.debate.service;

import java.io.IOException;
import java.util.UUID;

import com.likelion.realtalk.domain.debate.dto.DebateResultDto.AiSummaryResultDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebateRedisService {

  private final DebateResultService debateResultService;
  private final AiService aiService;
  private final SpeakerService speakerService;
  private final AudienceService audienceService;
  private final DebateRoomService debateRoomService;
  private final RoomCleanupService roomCleanupService;

  public void endDebate(String roomUUID) {
    // 토론방 종료 내용 DB 저장
    debateRoomService.endDebate(roomUUID);

    ArrayList<SpeakerMessageDto> speeches = speakerService.getSpeeches(roomUUID);

    try {
      AiSummaryResultDto resultDto = (speeches == null || speeches.isEmpty())
          ? AiSummaryResultDto.empty()
          : aiService.summaryResult(speeches);

      debateResultService.saveDebateResult(roomUUID, resultDto);
    } catch (IOException e) {
      // AI 요약 결과 저장 비동기로 5번 retry
      CompletableFuture.runAsync(() -> retrySummary(speeches, 5, roomUUID));
    }

    // speak 관련 redis 정보 삭제
    this.speakerService.clearSpeakerCaches(roomUUID);
    // audience 관련 redis 정보 삭제
    this.audienceService.clearAudienceCaches(roomUUID);
    // ai 관련 redis 정보 삭제
    this.aiService.clearAiCaches(roomUUID);
    // room 매핑 데이터 삭제
    roomCleanupService.cleanupParticipants(UUID.fromString(roomUUID));

    // WebSocket 연결 해지 요청
    debateRoomService.pubEndDebate(roomUUID);
  }

  private void retrySummary(ArrayList<SpeakerMessageDto> speeches, int maxRetries, String roomUUID) {
    int attempt = 0;

    while (attempt < maxRetries) {
      try {
        AiSummaryResultDto resultDto = (speeches == null || speeches.isEmpty())
            ? AiSummaryResultDto.empty()
            : aiService.summaryResult(speeches);

        debateResultService.saveDebateResult(roomUUID, resultDto);
        return;
      } catch (IOException e) {
        attempt++;
        log.warn("AI 요청 실패 (시도 {} / {}). 다시 시도합니다.", attempt, maxRetries, e);

        try {
          Thread.sleep(1000L); // 네트워크 문제일 수 있기 때문에 1초 쉬었다 다시 시도
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt(); // interrupt 상태를 다시 true로 바꿔주며 상태 복원
          log.error("재시도 중 인터럽트 발생. 처리 중단.", ie);
          return;
        }
      }
    }

    // 최대 재시도까지 실패하면 로그만 남김
    log.error("AI 요약 요청 최종 실패 ({}회 시도).", maxRetries);
    debateResultService.saveDebateResult(roomUUID, AiSummaryResultDto.failure());
  }
}
