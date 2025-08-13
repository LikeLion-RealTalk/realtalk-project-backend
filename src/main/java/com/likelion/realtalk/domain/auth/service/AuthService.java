package com.likelion.realtalk.domain.auth.service;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import com.likelion.realtalk.global.security.jwt.JwtCookieUtil;
import com.likelion.realtalk.global.security.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String REFRESH_COOKIE_NAME = "refresh_token";
  private static final Duration ACCESS_TTL = Duration.ofHours(1);
  private static final Duration REFRESH_TTL = Duration.ofDays(14);

  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;

  @Transactional
  public void reissueTokens(HttpServletRequest request, HttpServletResponse response) {
    // 1. 쿠키에서 refresh token 추출
    String refreshToken = findCookieValue(request, REFRESH_COOKIE_NAME)
        .orElseThrow(() -> new IllegalArgumentException("리프레시 토큰이 없습니다."));

    // 2. refresh token에서 사용자 정보 추출(userId)
    Long userId = jwtProvider.getUserId(refreshToken);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

    // 3. DB에 저장된 refresh token과 일치하는지 검증
    if (!refreshToken.equals(user.getRefreshToken())) {
      throw new SecurityException("리프레시 토큰이 유효하지 않습니다.");
    }

    // 4. 새 access/refresh 토큰 발급
    CustomUserDetails userDetails = new CustomUserDetails(user);
    String newAccessToken = jwtProvider.createToken(userDetails, ACCESS_TTL.toMillis()); // 1시간
    String newRefreshToken = jwtProvider.createToken(userDetails, REFRESH_TTL.toMillis()); // 14일

    // 5. DB에 refresh token 갱신
    user.updateRefreshToken(newRefreshToken);
    userRepository.save(user);

    // 6. 쿠키에 저장
    JwtCookieUtil.addAccessTokenCookie(request, response, newAccessToken, ACCESS_TTL);
    JwtCookieUtil.addRefreshTokenCookie(request, response, newRefreshToken, REFRESH_TTL);
  }

  private Optional<String> findCookieValue(HttpServletRequest request, String name) {
    if (request.getCookies() == null) return Optional.empty();
    return Arrays.stream(request.getCookies())
        .filter(c -> name.equals(c.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }

}
