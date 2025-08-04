package com.likelion.realtalk.domain.oauth.handler;

import static com.likelion.realtalk.global.security.jwt.JwtCookieUtil.*;

import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

  private final UserRepository userRepository;

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    // 액세스 토큰 쿠키 삭제
    deleteAccessTokenCookie(response);

    // 리프레시 토큰 쿠키, DB 삭제
    deleteRefreshTokenCookie(response);

    if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
      Long userId = userDetails.getUserId();
      userRepository.findById(userId).ifPresent(user -> {
        user.setRefreshToken(null);
        userRepository.save(user);
      });
    }

    // 로그아웃 성공 후 원하는 경로 리다이렉트 또는 JSON 응답 처리
//    response.setContentType("application/json");
//    response.getWriter().write("{\"message\": \"logout success\"}");
    response.sendRedirect("/login"); // 로그아웃 후 리다이렉트할 URL 설정

  }
}
