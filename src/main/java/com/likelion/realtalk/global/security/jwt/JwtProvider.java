package com.likelion.realtalk.global.security.jwt;

import com.likelion.realtalk.global.security.core.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        // 권한 정보를 토큰에 포함(없어도 동작은 가능하지만, 있으면 WebSocket 권한 세팅이 편합니다)
        .claim("roles", principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()))
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

  // alias: validate (WebSocket 쪽에서 이 이름으로 호출해도 되게)
  public boolean validate(String token) {
    return validateToken(token);
  }

  // 토큰에서 권한 목록 추출 (roles 클레임이 없으면 빈 리스트 반환)
  public Collection<? extends GrantedAuthority> getAuthorities(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();

    Object roles = claims.get("roles");
    if (roles instanceof List<?> list) {
      return list.stream()
          .map(String::valueOf)
          .map(SimpleGrantedAuthority::new)
          .collect(Collectors.toList());
    }
    return List.of();
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

  // 토큰에서 username 추출
  public String getUsername(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .get("username", String.class);
  }

  // 만료 여부 체크
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
