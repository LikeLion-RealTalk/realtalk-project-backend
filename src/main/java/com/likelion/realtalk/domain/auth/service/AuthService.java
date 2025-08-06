package com.likelion.realtalk.domain.auth.service;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import com.likelion.realtalk.global.security.jwt.JwtCookieUtil;
import com.likelion.realtalk.global.security.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;

  public void reissueTokens(HttpServletRequest request, HttpServletResponse response) {
    // 1. 쿠키에서 refresh token 추출
    String refreshToken = null;
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("refresh_token".equals(cookie.getName())) {
          refreshToken = cookie.getValue();
          break;
        }
      }
    }
    if (refreshToken == null) throw new IllegalArgumentException("리프레시 토큰 없음");

    // 2. refresh token에서 사용자 정보 추출(예: userId)
    Long userId = jwtProvider.getUserId(refreshToken);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

    // 3. DB에 저장된 refresh token과 일치하는지 검증
    if (!refreshToken.equals(user.getRefreshToken())) {
      throw new SecurityException("리프레시 토큰이 유효하지 않습니다.");
    }

    // 4. 새 access/refresh 토큰 발급
    CustomUserDetails userDetails = new CustomUserDetails(user);
    String newAccessToken = jwtProvider.createToken(userDetails, 60 * 60 * 1000L); // 1시간
    String newRefreshToken = jwtProvider.createToken(userDetails, 60 * 60 * 24 * 14 * 1000L); // 14일

    // 5. DB에 refresh token 갱신
    user.setRefreshToken(newRefreshToken);
    userRepository.save(user);

    // 6. 쿠키에 저장
    JwtCookieUtil.addAccessTokenCookie(response, newAccessToken, 60 * 60);
    JwtCookieUtil.addRefreshTokenCookie(response, newRefreshToken, 60 * 60 * 24 * 14);
  }

}
