package com.likelion.realtalk.global.config;

import com.likelion.realtalk.domain.debate.service.RecordingService;
import com.likelion.realtalk.infra.handler.SpeechBinaryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;


/**
 * 녹음 시작할 때 프론트에서 음성 파일 받아오기 위해 필요한 바이너리 전용 websocketconfig 설정입니다.
 *
 * @author : 오승훈
 * @fileName : WebSocketSpeechConfig
 * @since : 2025-08-01
 */

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketSpeechConfig implements WebSocketConfigurer {

  private final SpeechBinaryHandler speechBinaryHandler; // <- 기존 RecordingService 대신 주입

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(speechBinaryHandler, "/ws/speech")
        // .addInterceptors(userIdHandshakeInterceptor()) // (선택) 쿼리/헤더 검증 시
        .setAllowedOriginPatterns("*"); // setAllowedOrigins("*") 대신 최신 메서드
    // SockJS는 바이너리 오디오에 불리하므로 미사용(순수 WS 유지)
  }

  // (선택) 큰 바이너리 처리 시 버퍼 상향
  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    var container = new ServletServerContainerFactoryBean();
    container.setMaxBinaryMessageBufferSize(1024 * 1024); // 1MB per frame
    container.setMaxSessionIdleTimeout(60_000L);          // 60s idle
    return container;
  }
}