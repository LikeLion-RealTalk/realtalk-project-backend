package com.likelion.realtalk.domain.oauth.handler;

import static com.likelion.realtalk.global.security.jwt.JwtCookieUtil.*;

import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.security.jwt.JwtProvider;
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
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

  @Value("${frontend.url}")
  private String frontendUrl;

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public void onLogoutSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    log.info("=== OAuth2 로그아웃 핸들러 시작 ===");

    // 쿠키에서 refreshToken 추출
    String refreshToken = null;
    if (request.getCookies() != null) {
      for (var c : request.getCookies()) {
        if (REFRESH_TOKEN_COOKIE_NAME.equals(c.getName())) {
          refreshToken = c.getValue();
          break;
        }
      }
    }

    // refreshToken으로 userId 추출 및 DB에서 해당 user의 refreshToken 제거
    if (refreshToken != null && !refreshToken.isBlank()) {
      try {
        Long uid = jwtProvider.getUserId(refreshToken);
        if (uid != null) {
          userRepository.clearRefreshTokenById(uid);
          log.info("✅ userId={} 의 refreshToken 필드 삭제 완료", uid);
        } else {
          log.warn("⚠️ refreshToken에서 userId 추출 실패");
        }
      } catch (Exception e) {
        log.warn("⚠️ refreshToken DB 삭제 중 예외 발생: {}", e.getMessage());
      }
    }


    // 리프레시 토큰 쿠키 삭제
    deleteRefreshTokenCookie(request, response);

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
