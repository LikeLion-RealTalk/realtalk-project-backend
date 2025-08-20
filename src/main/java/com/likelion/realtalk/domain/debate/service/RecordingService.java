package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.dto.DebateMessageDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import com.likelion.realtalk.domain.debate.type.Side;
import com.likelion.realtalk.infra.handler.SpeechBinaryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 음성 발언 제어 및 STT 처리 서비스
 *
 * mode:
 *  - "녹음 시작": WebSocket 버퍼 초기화 및 녹음 상태 ON
 *  - "녹음 중지": 녹음 상태 OFF (clear=true → 버퍼 초기화)
 *  - "종료": 누적 오디오 STT 변환 후 /topic 송출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

  private final SpeechBinaryHandler binaryHandler;
  private final SpeechToTextService speechToTextService;
  private final SimpMessagingTemplate messagingTemplate;
  private final SpeakerService speakerService;

  public void handleControl(DebateMessageDto payload) {
    // 현재 발언 시간이 아닐 경우 예외 처리
//    speakerService.validateSpeaker(payload);

    final Long userId = payload.getUserId();
    final String roomUUID = payload.getRoomUUID();
    final String mode = payload.getMode();
    final Side side = payload.getSide();

    switch (mode) {

      case "녹음 시작" -> {
        // roomUUID만 저장 (username/side 필요 없음)
        SpeechBinaryHandler.DebateStartMeta meta =
            new SpeechBinaryHandler.DebateStartMeta(roomUUID, null, side);
        binaryHandler.start(userId, meta);
        log.info("🎙️ 녹음 시작 userId={}, roomUUID={}", userId, roomUUID);
      }

      case "녹음 중지" -> {
        binaryHandler.stop(userId, true);
        log.info("⏸️ 녹음 중지 userId={}", userId);
      }

      case "종료" -> {
        // 누적 오디오 추출
        byte[] audio = binaryHandler.end(userId);
        if (audio.length == 0) {
          log.warn("🏁 종료 요청 but 오디오 없음 (userId={})", userId);
          return;
        }

        // STT 변환
        String transcript = speechToTextService.recognize(audio);

        // 변환된 텍스트를 speaker 서비스에 넘겨줄 dto에 값 저장
        payload.setMessage(transcript);
        
        // SpeakerMessageDto 생성
        // speaker 서비스에 텍스트, userid, roomid 프론트에서 받아서 넘겨주면 speaker 서비스에서 만들어서 보내주는거 message dto로 받아서 구독자들(프론트)에게 넘겨줌
        speakerService.submitSpeech(payload);

        log.info("🟢 발언 broadcast room={}, userId={}", roomUUID, userId);
      }

      default -> log.warn("알 수 없는 mode: {}", mode);
    }
  }
}
