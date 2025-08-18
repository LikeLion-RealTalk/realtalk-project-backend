package com.likelion.realtalk.global.security.jwt;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.likelion.realtalk.global.security.core.CustomUserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

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
    // 2) userId 안전 추출: Integer/Long/String 모두 처리 + "id" 키 폴백
    public Long getUserId(String token) {
        Claims c = parseClaims(token);
        Object v = c.get("userId"); if (v == null) v = c.get("id");
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Long l)    return l;
        if (v instanceof String s && !s.isBlank()) return Long.parseLong(s.trim());
        return null;
    }

    // 3) username 추출: 없으면 subject(sub)로 폴백
    public String getUsername(String token) {
        Claims c = parseClaims(token);
        String u = c.get("username", String.class);
        if (u == null || u.isBlank()) u = c.getSubject();
        return u;
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
    // 추가: 공용 클레임 파서 (이미 있으면 그거 사용)
    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token).getPayload();
    }
}
