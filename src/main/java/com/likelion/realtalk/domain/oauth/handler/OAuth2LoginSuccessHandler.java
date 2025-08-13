package com.likelion.realtalk.domain.oauth.handler;

import static com.likelion.realtalk.global.security.jwt.JwtCookieUtil.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.exception.UserNotFoundException;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import com.likelion.realtalk.global.security.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Value("${jwt.access-token-expiry}")
  private Long accessTokenExpiry;

  @Value("${jwt.refresh-token-expiry}")
  private Long refreshTokenExpiry;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    log.info("=== OAuth2 로그인 성공 핸들러 시작 ===");

    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("X-Content-Type-Options", "nosniff");

    // 인증 성공한 유저 정보 추출
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    log.info("사용자 정보: {}", userDetails.getUser().getUsername());

    // JWT 토큰 생성
    String accessToken = jwtProvider.createToken(userDetails, accessTokenExpiry);
    String refreshToken = jwtProvider.createToken(userDetails, refreshTokenExpiry);

    // 쿠키에 토큰 저장
    addRefreshTokenCookie(request, response, refreshToken, Duration.ofMillis(refreshTokenExpiry));

    log.info("JWT 토큰 생성 및 쿠키 설정 완료");

    User user = userRepository.findById(userDetails.getUserId())
        .orElseThrow(UserNotFoundException::new);
    user.updateRefreshToken(refreshToken);
    userRepository.save(user);

    // 로그
    log.info("✅ OAuth2 로그인 성공: user={}, access token 발급", userDetails.getUsername());

    // JSON 바디로 Access Token 응답
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    long expireTime = Duration.ofMillis(accessTokenExpiry).toSeconds();
    ObjectNode body = JsonNodeFactory.instance.objectNode();
    body.put("accessToken", accessToken);
    body.put("tokenType", "Bearer");
    body.put("expiresIn", expireTime);

    objectMapper.writeValue(response.getWriter(), body);

    // 테스트 페이지로 리다이렉트
    // response.sendRedirect(frontendUrl + "/oauth2/test?success=true");
  }
}
