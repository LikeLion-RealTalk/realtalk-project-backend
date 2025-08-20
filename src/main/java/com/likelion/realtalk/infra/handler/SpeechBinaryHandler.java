package com.likelion.realtalk.infra.handler;

import com.likelion.realtalk.domain.debate.type.Side;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * 바이너리 오디오 스트림에 대해 처리하는 순수 WebSocket 핸들러
 *
 * @author : 오승훈
 * @fileName : SpeechBinaryHandler
 * @since : 2025-08-09
 */
@Slf4j
@Component
public class SpeechBinaryHandler extends BinaryWebSocketHandler {

  // userId -> 세션 컨텍스트
  private final Map<Long, SessionContext> userCtx = new ConcurrentHashMap<>();

  // sessionId -> userId (역참조용)
  private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Long userId = extractUserId(session); // ?senderId=123 같은 쿼리
    if (userId == null) {
      log.warn("❗ userId 누락: 세션 종료");
      try { session.close(CloseStatus.BAD_DATA); } catch (Exception ignored) {}
      return;
    }
    userCtx.putIfAbsent(userId, new SessionContext(userId, session));
    sessionToUser.put(session.getId(), userId);
    log.info("✅ 연결됨 userId={}, sessionId={}", userId, session.getId());
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    Long userId = sessionToUser.get(session.getId());
    if (userId == null) return;

    SessionContext ctx = userCtx.get(userId);
    if (ctx == null || !ctx.recording.get()) return; // 녹음 중일 때만 누적

    try {
      byte[] chunk = message.getPayload().array();
      synchronized (ctx.buffer) {
        ctx.buffer.write(chunk);
      }
      log.debug("🎧 {} bytes 누적 (userId={})", chunk.length, userId);
    } catch (Exception e) {
      log.error("버퍼 누적 에러", e);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Long userId = sessionToUser.remove(session.getId());
    if (userId != null) {
      userCtx.remove(userId);
      log.info("❌ 연결 종료 userId={}, sessionId={}", userId, session.getId());
    }
  }

  // ---- 제어 API (서비스에서 호출) ----
  public void start(Long userId, DebateStartMeta meta) {
    SessionContext ctx = userCtx.computeIfAbsent(userId, id -> new SessionContext(id, null));
    ctx.recording.set(true);
    ctx.meta = meta;                // roomUUID/side/username 등 저장
    ctx.buffer.reset();             // 새 녹음 시작 시 버퍼 초기화
    log.info("🎙️ 녹음 시작 userId={}, room={}", userId, meta.roomUUID());
  }

  /** 중지: UI 요구대로 '말한 거 초기화'가 필요하면 clear=true 로 호출 */
  public void stop(Long userId, boolean clear) {
    SessionContext ctx = userCtx.get(userId);
    if (ctx == null) return;
    ctx.recording.set(false);
    if (clear) ctx.buffer.reset();  // 취소 느낌
    log.info("⏸️ 녹음 중지 userId={}, cleared={}", userId, clear);
  }

  /** 종료: 누적된 오디오 반환 후 내부 상태는 초기화 */
  public byte[] end(Long userId) {
    SessionContext ctx = userCtx.get(userId);
    if (ctx == null) return new byte[0];

    ctx.recording.set(false);
    byte[] audio;
    synchronized (ctx.buffer) {
      audio = ctx.buffer.toByteArray();
    }
    ctx.buffer.reset();   // 한 번 보낸 뒤 비움
    log.info("🏁 녹음 종료 userId={}, bytes={}", userId, audio.length);
    return audio;
  }

  public DebateStartMeta currentMeta(Long userId) {
    SessionContext ctx = userCtx.get(userId);
    return (ctx != null) ? ctx.meta : null;
  }

  private Long extractUserId(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null || uri.getQuery() == null) return null;
    for (String kv : uri.getQuery().split("&")) {
      String[] parts = kv.split("=");
      if (parts.length == 2 && ("senderId".equals(parts[0]) || "userId".equals(parts[0]))) {
        try { return Long.parseLong(parts[1]); } catch (NumberFormatException ignored) {}
      }
    }
    return null;
  }

  // ---- 내부 타입 ----
  @Getter
  static class SessionContext {
    final Long userId;
    final WebSocketSession session;                // 필요 시 사용
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final AtomicBoolean recording = new AtomicBoolean(false);
    volatile DebateStartMeta meta;                 // 시작 시점 메타 저장

    SessionContext(Long userId, WebSocketSession session) {
      this.userId = userId;
      this.session = session;
    }
  }

  /** 시작 시점 메타 (STOMP payload에서 넘어온 값) */
  public record DebateStartMeta(String roomUUID, String username, Side side) {}
}
