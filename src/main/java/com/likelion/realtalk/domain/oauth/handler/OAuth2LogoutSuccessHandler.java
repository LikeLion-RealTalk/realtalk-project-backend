package com.likelion.realtalk.domain.oauth.handler;

import static com.likelion.realtalk.global.security.jwt.JwtCookieUtil.*;

import com.likelion.realtalk.domain.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

  @Value("${frontend.url}")
  private String frontendUrl;

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

    // 리다이렉트
    // 1. redirect_uri 파라미터 확인
    String redirectUri = request.getParameter("redirect_uri");

    // 2. 없으면 홈(/)으로, 있으면 그 경로로(보안상 /로 시작만 허용)
    String targetUrl = frontendUrl + "/";
    if (redirectUri != null && !redirectUri.isBlank() && redirectUri.startsWith("/")) {
      targetUrl = frontendUrl + redirectUri;
    }

    response.sendRedirect(targetUrl);

    // 로그아웃 후 테스트 페이지로 리다이렉트
//    response.sendRedirect(frontendUrl + "/oauth2/test");
  }
}
