package com.likelion.realtalk.global.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;


public class JwtCookieUtil {

  public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
  public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

  private static boolean isLocal(String host) {
    return "localhost".equalsIgnoreCase(host) || host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
  }

  private static String topDomainIfApplicable(String host) {
    return host != null && host.endsWith("realtalks.co.kr") ? "realtalks.co.kr" : null;
  }

  public static void addAccessTokenCookie(HttpServletRequest req, HttpServletResponse res, String token, Duration ttl) {
    String host = req.getServerName();
    String domain = topDomainIfApplicable(host);

    ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
        .httpOnly(true)
        .secure(false)
        .path("/")
        .maxAge(ttl)
        .sameSite("Lax"); // www/api는 same-site라 Lax로 충분

    if (domain != null) b.domain(domain); // localhost면 미지정(Host-only)

    res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
  }

  public static void addRefreshTokenCookie(HttpServletRequest req, HttpServletResponse res, String token, Duration ttl) {
    String host = req.getServerName();
    String domain = topDomainIfApplicable(host);

    ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
        .httpOnly(true)
        .secure(false)
        .path("/")
        .maxAge(ttl)
        .sameSite("Lax");

    if (domain != null) b.domain(domain);

    res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
  }

  public static void deleteAccessTokenCookie(HttpServletRequest req, HttpServletResponse res) {
    String host = req.getServerName();
    String domain = topDomainIfApplicable(host);

    ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(false)
        .path("/")
        .maxAge(0)
        .sameSite("Lax");

    if (domain != null) b.domain(domain);

    res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
  }

  public static void deleteRefreshTokenCookie(HttpServletRequest req, HttpServletResponse res) {
    String host = req.getServerName();
    String domain = topDomainIfApplicable(host);

    ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(false)
        .path("/")
        .maxAge(0)
        .sameSite("Lax");

    if (domain != null) b.domain(domain);

    res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
  }
}
