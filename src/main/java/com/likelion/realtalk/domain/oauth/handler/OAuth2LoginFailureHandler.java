package com.likelion.realtalk.domain.oauth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper;

  @Value("${frontend.url}")
  private String frontendUrl;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    log.info("OAuth2 로그인 실패: {}", exception.getClass().getSimpleName());
    log.info("OAuth2 로그인 실패 상세: {}", exception.getMessage());

    // XHR/JSON 요청이면 JSON 401 반환, 그 외에는 프론트 도메인으로 리다이렉트
    if (isApiOrAjaxRequest(request)) {
      // 실패 응답 객체 구성
      Map<String, Object> body = new HashMap<>();
      body.put("code", "OAUTH2_LOGIN_FAILED");
      body.put("message", "소셜 로그인에 실패했습니다.");

      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write(objectMapper.writeValueAsString(body));
      return;
    }
    // 브라우저 내비게이션인 경우: 프론트 페이지로 안전하게 절대 URL 리다이렉트
    String redirect = buildFailureRedirectUrl(exception);
    response.setStatus(HttpServletResponse.SC_FOUND); // 302
    response.setHeader("Location", redirect);
  }

  private boolean isApiOrAjaxRequest(HttpServletRequest request) {
    String xrw = request.getHeader("X-Requested-With");
    String accept = request.getHeader("Accept");
    return (xrw != null && xrw.equalsIgnoreCase("XMLHttpRequest"))
        || (accept != null && accept.toLowerCase().contains("application/json"));
  }

  private String buildFailureRedirectUrl(AuthenticationException exception) {
    // 절대 URL 사용 (www 도메인, https 고정)
    String base = frontendUrl != null ? frontendUrl.trim() : "https://www.realtalks.co.kr";
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base;
  }
}
