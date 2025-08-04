package com.likelion.realtalk.global.security.jwt;

import com.likelion.realtalk.global.security.core.CustomUserDetails;
import io.jsonwebtoken.*;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  private final SecretKey secretKey;

  // 토큰 발급
  public String createToken(CustomUserDetails principal, Long expiryTime) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expiryTime);

    return Jwts.builder()
        .subject(principal.getUsername())
        .claim("userId", principal.getUserId())
        .claim("username", principal.getUsername())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(secretKey, Jwts.SIG.HS256)
        .compact();
  }

  // 토큰 유효성 검증
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  // 토큰에서 사용자 ID 추출
  public Long getUserId(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .get("userId", Long.class);
  }

  // 토큰에서 username 추출 (예: 인증 과정에서 필요시)
  public String getUsername(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .get("username", String.class);
  }

  // 토큰 만료 여부 체크 (선택)
  public boolean isTokenExpired(String token) {
    Date expiration = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getExpiration();
    return expiration.before(new Date());
  }

}
