package com.likelion.realtalk.domain.oauth.handler;

import static com.likelion.realtalk.global.security.jwt.JwtCookieUtil.*;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.exception.UserNotFoundException;
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
  private final UserRepository userRepository;

  @Value("${jwt.access-token-expiry}")
  private Long accessTokenExpiry;

  @Value("${jwt.refresh-token-expiry}")
  private Long refreshTokenExpiry;

  @Value("${frontend.url}")
  private String frontendUrl;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    log.info("=== OAuth2 로그인 성공 핸들러 시작 ===");

    // 인증 성공한 유저 정보 추출
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    log.info("사용자 정보: {}", userDetails.getUser().getUsername());

    // JWT 토큰 생성
    String accessToken = jwtProvider.createToken(userDetails, accessTokenExpiry);
    String refreshToken = jwtProvider.createToken(userDetails, refreshTokenExpiry);

    // 쿠키에 토큰 저장
    addAccessTokenCookie(response, accessToken, accessTokenExpiry.intValue() / 1000);
    addRefreshTokenCookie(response, refreshToken, refreshTokenExpiry.intValue() / 1000);

    log.info("JWT 토큰 생성 및 쿠키 설정 완료");

    User user = userRepository.findById(userDetails.getUserId())
        .orElseThrow(UserNotFoundException::new);
    user.updateRefreshToken(refreshToken);
    userRepository.save(user);

    // 로그
    log.info("✅ OAuth2 로그인 성공: user={}, access token 발급", userDetails.getUsername());

    // 리다이렉트
    // 1. redirect_uri 파라미터 받기
    String redirectUri = request.getParameter("redirect_uri");

    // 2. 기본 경로 설정 (없으면 홈으로)
    String targetUrl = frontendUrl + "/";
    if (redirectUri != null && !redirectUri.isBlank()) {
      // 보안상 내 도메인에서 시작하는지 체크
      if (redirectUri.startsWith("/")) {
        targetUrl = frontendUrl + redirectUri;
      }
    }

    response.sendRedirect(targetUrl);

    // 테스트 페이지로 리다이렉트
//    response.sendRedirect(frontendUrl + "/oauth2/test?success=true");
  }
}
