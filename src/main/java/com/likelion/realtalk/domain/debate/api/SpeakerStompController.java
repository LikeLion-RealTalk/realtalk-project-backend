package com.likelion.realtalk.domain.debate.api;
import com.likelion.realtalk.domain.debate.dto.DebateMessageDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import com.likelion.realtalk.domain.debate.service.SpeakerService;
//import com.likelion.realtalk.domain.speaker.service.SpeakerStompService;
//import com.likelion.realtalk.domain.speaker.service.SpeechToTextService;
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
//  private final SpeakerStompService speakerStompService;
  private final SpeakerService speakerService;


  /**
   * 📝 텍스트 발언 수신
   * 클라이언트 → /pub/speaker/text
   */
  @MessageMapping("/speaker/text")
  public void handleTextSpeak(DebateMessageDto payload) {
    log.info("텍스트 발언 수신: {}", payload.getMessage());
    log.info("user id: {}", payload.getUserId());
    log.info("room id: {}", payload.getRoomUUID());

    String roomUUID = payload.getRoomUUID();
    // speaker 서비스에 텍스트, userid, roomid 프론트에서 받아서 넘겨주면 speaker 서비스에서 만들어서 보내주는거 message dto로 받아서 구독자들(프론트)에게 넘겨줌
    SpeakerMessageDto message = speakerService.submitSpeech(roomUUID,payload);

    messagingTemplate.convertAndSend("/topic/speaker/" + roomUUID, message);
  }
  
  
//  //여기 수정하기
//  /**
//   * 🎙️ 음성 발언 수신 + STT 처리
//   * 클라이언트 → /pub/speaker/voice
//   */
//  @MessageMapping("/speaker/voice")//클라이언트가 서버로 보내는 앤드포인트
//  public void handleVoiceSpeak(SpeakerVoicePayload payload) {
//
//    speakerStompService.speakerService(messagingTemplate, payload);
//
//    // speaker 서비스에 텍스트, userid, roomid 프론트에서 받아서 넘겨주면 speaker 서비스에서 만들어서 보내주는거 message dto로 받아서 구독자들(프론트)에게 넘겨줌
//    //SpeakerMessage message = aiValidationService.validate();
//
//    messagingTemplate.convertAndSend("/topic/speaker/" + payload.getRoomId(), message); //서바가 클라이언트로 보내는 앤드포인트
//
//  }

}