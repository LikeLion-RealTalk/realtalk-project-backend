package com.likelion.realtalk.domain.oauth.handler;

import static com.likelion.realtalk.global.security.jwt.JwtCookieUtil.*;

import com.likelion.realtalk.global.security.core.CustomUserDetails;
import com.likelion.realtalk.global.security.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

  @Value("${spring.jwt.access-token-expiry}")
  private Long accessTokenExpiry;

  @Value("${spring.jwt.refresh-token-expiry}")
  private Long refreshTokenExpiry;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // 인증 성공한 유저 정보 추출
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

    // 토큰들 발급
    String accessToken = jwtProvider.createToken(userDetails, accessTokenExpiry);
    String refreshToken = jwtProvider.createToken(userDetails, refreshTokenExpiry);

    // 쿠키에 저장
    addAccessTokenCookie(response, accessToken, accessTokenExpiry.intValue() / 1000);
    addRefreshTokenCookie(response, refreshToken, refreshTokenExpiry.intValue() / 1000);

    // 로그
    log.info("✅ OAuth2 로그인 성공: user={}, access token 발급", userDetails.getUsername());

    // 리다이렉트 또는 JSON 응답
    response.sendRedirect("/"); // 로그인 후 리다이렉트할 URL 설정
//    response.setContentType("application/json");
//    response.setCharacterEncoding("UTF-8");
//    response.getWriter().write("{\"message\": \"로그인 성공\", \"accessToken\"}");
  }
}
