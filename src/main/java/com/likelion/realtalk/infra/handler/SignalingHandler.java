package com.likelion.realtalk.infra.handler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket을 통해 연결된 클라이언트들 간에 메시지를 실시간으로 전달(브로드캐스트)하는 역할을 담당하는 핸들러입니다.
 *
 * @author : 오승훈
 * @fileName : SignalingHandler
 * @since : 2025-08-01
 */
/**
 * 현재 연결된 모든 클라이언트(WebSocket 세션)를 저장하는 Set
 * - CopyOnWriteArraySet 사용 이유:
 *   1) 멀티스레드 환경에서 안전하게 읽기/쓰기 가능
 *   2) 중복 연결 방지
 */

@Slf4j
public class SignalingHandler extends TextWebSocketHandler {

  /**
   * 현재 연결된 모든 클라이언트(WebSocket 세션)를 저장하는 Set
   * - CopyOnWriteArraySet 사용 이유:
   *   1) 멀티스레드 환경에서 안전하게 읽기/쓰기 가능
   *   2) 중복 연결 방지
   */
  private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

  /**
   * 클라이언트가 WebSocket 연결을 맺었을 때 호출됩니다.
   * - 새로운 세션을 sessions에 추가하여 이후 메시지를 주고받을 수 있게 함.
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    sessions.add(session);
    System.out.println("Connected: " + session.getId());
  }

  /**
   * 클라이언트로부터 텍스트 메시지를 받았을 때 호출됩니다.
   * - 현재는 단순히 모든 다른 연결된 클라이언트에게 메시지를 전달(브로드캐스트)합니다.
   * - STT의 경우: 여기서 받은 텍스트를 음성으로 변환(TTS) 요청 후 바이너리로 전송 가능.
   * - TTS의 경우: 여기서 받은 음성 데이터(STT 결과)를 텍스트로 변환하여 전송 가능.
   */
  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String payload = message.getPayload();

    // 🔹 기존: 단순 텍스트만 브로드캐스트
    // for (WebSocketSession s : sessions) { ... }

    // ✅ 변경: 텍스트와 함께 TTS URL 포함해서 브로드캐스트
    String senderId = extractSenderId(session); // senderId 가져오기
    String json = String.format(
        "{\"type\":\"text\", \"senderId\":\"%s\", \"text\":\"%s\", \"audioUrl\":\"/api/tts/speak?text=%s\"}",
        senderId.replaceAll("\"", ""),
        payload.replaceAll("\"", ""),
        java.net.URLEncoder.encode(payload, "UTF-8")
    );

    for (WebSocketSession s : sessions) {
      if (s.isOpen()) {
        s.sendMessage(new TextMessage(json));
      }
    }
  }

  /**
   * 클라이언트의 WebSocket 연결이 끊어졌을 때 호출됩니다.
   * - sessions에서 해당 세션을 제거하여 불필요한 리소스 점유를 방지합니다.
   */
  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessions.remove(session);
    System.out.println("Disconnected: " + session.getId());
  }

  private String extractSenderId(WebSocketSession session) {
    try {
      String query = session.getUri().getQuery();
      if (query != null && query.startsWith("senderId=")) {
        return query.substring("senderId=".length());
      }
    } catch (Exception e) {
      log.warn("senderId 추출 실패", e);
    }
    return "anonymous";
  }
}