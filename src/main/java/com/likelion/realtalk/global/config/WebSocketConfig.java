package com.likelion.realtalk.global.config;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.likelion.realtalk.domain.debate.auth.AuthUserPrincipal;
import com.likelion.realtalk.domain.debate.auth.GuestPrincipal;
import com.likelion.realtalk.domain.webrtc.handler.SignalingHandler;
import com.likelion.realtalk.global.security.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

  private final SignalingHandler signalingHandler;


  private final JwtProvider jwt;  

  // public WebSocketConfig(SignalingHandler signalingHandler) {
  //   this.signalingHandler = signalingHandler;
  // }

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

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
            var acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (acc == null) return message;

            if (StompCommand.CONNECT.equals(acc.getCommand())) {
                String h = first(acc.getNativeHeader("Authorization"));
                if (h == null) h = first(acc.getNativeHeader("authorization"));
                System.out.println("[STOMP][DBG] hasHeader=" + (h!=null) + " endpoint=" + acc.getDestination());

                    boolean authenticated = false;
                if (h != null && h.startsWith("Bearer ")) {
                    String token = h.substring(7);
                    boolean valid = jwt.validate(token);
                    System.out.println("[STOMP][DBG] jwt.validate=" + valid);
                    if (valid) {
                    Long uid = jwt.getUserId(token);
                    String uname = jwt.getUsername(token);
                    System.out.println("[STOMP][DBG] parsed uid=" + uid + " uname=" + uname);
                    if (uid != null && uname != null && !uname.isBlank()) {
                        var principal = new AuthUserPrincipal(uid, uname, jwt.getAuthorities(token));
                        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        acc.setUser(auth);
                        authenticated = true;
                        System.out.println("[STOMP][DBG] set AuthUserPrincipal OK");
                    }
                    }
                }
                if (!authenticated) {
                    String suf = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    var principal = new GuestPrincipal("guest-" + suf, "게스트 " + suf);
                    var auth = new UsernamePasswordAuthenticationToken(principal, null); // 미인증
                    acc.setUser(auth);
                    System.out.println("[STOMP][DBG] set GuestPrincipal (unauthenticated)");
                }
            }
            return message;
            }
            private String first(java.util.List<String> list) { return (list != null && !list.isEmpty()) ? list.get(0) : null; }
        });
    }

}
