package com.likelion.realtalk.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.likelion.realtalk.domain.webrtc.handler.SignalingHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

  private final SignalingHandler signalingHandler;

  public WebSocketConfig(SignalingHandler signalingHandler) {
    this.signalingHandler = signalingHandler;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // 서버 -> 클라이언트 구독 경로
    registry.enableSimpleBroker("/topic", "/sub"); // ← '/sub:' 오타 수정
    // 클라이언트 -> 서버 전송 경로 접두어 (@MessageMapping)
    registry.setApplicationDestinationPrefixes("/pub");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // STOMP 엔드포인트들
    registry.addEndpoint("/ws-debate")
        .setAllowedOriginPatterns("*") // TODO: 운영 시 구체 도메인으로 제한
        .withSockJS();

    registry.addEndpoint("/ws-speech")
        .setAllowedOriginPatterns("*")
        .withSockJS();

    registry.addEndpoint("/ws-stomp")
        .setAllowedOriginPatterns("*")
        .withSockJS();
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // WebRTC 시그널링(plain WebSocket, 비-STOMP)
    registry.addHandler(signalingHandler, "/ws-signaling")
        .setAllowedOriginPatterns("*"); // TODO: 운영 시 도메인 제한
  }
}
