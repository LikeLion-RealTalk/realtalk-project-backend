package com.likelion.realtalk.global.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;


public class JwtCookieUtil {

  public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
  public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

  // 액세스 토큰 쿠키 추가
  public static void addAccessTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
    Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, token);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(maxAgeSeconds);
    response.addCookie(cookie);
  }

  // 리프레시 토큰 쿠키 추가
  public static void addRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
    Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, token);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(maxAgeSeconds);
    response.addCookie(cookie);
  }

  // 액세스 토큰 쿠키 삭제
  public static void deleteAccessTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, null);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }

  // 리프레시 토큰 쿠키 삭제
  public static void deleteRefreshTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }
}
