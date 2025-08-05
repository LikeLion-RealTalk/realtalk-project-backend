package com.likelion.realtalk.domain.oauth.handler;

import static com.likelion.realtalk.global.security.jwt.JwtCookieUtil.*;

import com.likelion.realtalk.domain.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

  private final UserRepository userRepository;

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    log.info("=== OAuth2 로그아웃 핸들러 시작 ===");

    // 액세스 토큰 쿠키 삭제
    deleteAccessTokenCookie(response);

    // 리프레시 토큰 쿠키 삭제
    deleteRefreshTokenCookie(response);

    log.info("✅ 로그아웃 완료: 토큰 및 쿠키 삭제");

    // 로그아웃 후 테스트 페이지로 리다이렉트
    response.sendRedirect("/oauth2/test");
  }
}
