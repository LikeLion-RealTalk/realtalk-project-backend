package com.likelion.realtalk.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // 서버 -> 클라이언트 구독 경로: 두 설정 모두 유지 (/topic, /sub)
    registry.enableSimpleBroker("/topic", "/sub");

    // 클라이언트 -> 서버 전송 경로 접두어 (공통)
    registry.setApplicationDestinationPrefixes("/pub");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // 기존 모든 엔드포인트 유지
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
}
