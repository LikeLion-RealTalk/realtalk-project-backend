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
  public String createToken(Authentication auth, Long expiryTime) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expiryTime);

    CustomUserDetails customUserDetails = (CustomUserDetails) auth.getPrincipal();

    Claims claims = (Claims) Jwts.claims();
    claims.put("userId", customUserDetails.getUserId());
    claims.put("username", customUserDetails.getUsername());

    return Jwts.builder()
        .subject(customUserDetails.getUsername())
        .claims(claims)
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
  public Long getUserIdFromToken(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .get("userId", Long.class);
  }

}
