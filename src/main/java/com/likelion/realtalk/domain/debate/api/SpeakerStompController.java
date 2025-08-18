package com.likelion.realtalk.domain.debate.api;
import com.likelion.realtalk.domain.debate.dto.DebateMessageDto;
import com.likelion.realtalk.domain.debate.service.RecordingService;
import com.likelion.realtalk.domain.debate.service.SpeakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 발언자의 발언에 대해 stomp 처리하는 컨트롤러입니다.
 *
 * @author : 오승훈
 * @fileName : SpeakerStompController
 * @since : 2025-08-07
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class SpeakerStompController {

  private final SimpMessagingTemplate messagingTemplate;
//  private final SpeechToTextService speechToTextService;
  private final RecordingService recordingService;
  private final SpeakerService speakerService;


  /**
   * 📝 텍스트 발언 수신
   * 클라이언트 → /pub/speaker/text
   */
  @MessageMapping("/speaker/text")
  public void handleTextSpeak(DebateMessageDto payload) {
    // 현재 발언 시간이 아닐 경우 예외 처리
//    speakerService.validateSpeaker(payload);

    log.info("텍스트 발언 수신: {}", payload.getMessage());
    log.info("user id: {}", payload.getUserId());
    log.info("room id: {}", payload.getRoomUUID());

    // speaker 서비스에 텍스트, userid, roomid 프론트에서 받아서 넘겨주면 speaker 서비스에서 만들어서 보내주는거 message dto로 받아서 구독자들(프론트)에게 넘겨줌
    speakerService.submitSpeech(payload);

  }


  /**
   * 🎙️ 음성 발언 제어 (녹음 시작/중지/종료)
   * 클라이언트 → /pub/speaker/voice
   * publish는 RecordingService 안에서 수행됨
   */
  @MessageMapping("/speaker/voice")
  public void handleVoiceSpeak(DebateMessageDto payload) {
    recordingService.handleControl(payload); // ✅ 이 한 줄이면 끝 (중복 송출 금지)
  }

}