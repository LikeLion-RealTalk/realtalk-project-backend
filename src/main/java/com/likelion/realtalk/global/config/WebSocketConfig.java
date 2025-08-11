package com.likelion.realtalk.global.config;


import com.likelion.realtalk.domain.webrtc.handler.SignalingHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    /** 서버가 보내는 메시지를 클라이언트가 구독할 때 사용하는 경로 **/
    registry.enableSimpleBroker("/topic", "/sub:"); // 구독용 경로 서버 -> 클라이언트

    /**클라이언트가 서버에 메시지를 보낼 때 사용하는 경로 접두어   ->   @MessageMapping **/
    registry.setApplicationDestinationPrefixes("/pub"); //  클라이언트 ->  서버
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // 토론방 연결 endpoint
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
