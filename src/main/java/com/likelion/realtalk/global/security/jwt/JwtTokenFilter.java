package com.likelion.realtalk.global.security.jwt;

import com.likelion.realtalk.global.security.core.CustomUserDetailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailService customUserDetailService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getServletPath();
        String method = request.getMethod();
        log.info("🔍 JWT Filter 처리 시작: {} {}", method, path);

        String token = getTokenFromRequest(request);
        log.info("📋 추출된 토큰: {}", token != null ? "존재함" : "없음");

        if (token != null && jwtProvider.validateToken(token)) {
            try {
                UsernamePasswordAuthenticationToken authenticationToken = getAuthenticationToken(token);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                log.info("✅ 인증 성공: user={}", authenticationToken.getName());
            } catch (Exception e) {
                log.error("❌ 인증 토큰 생성 실패: {}", e.getMessage());
            }
        } else {
            log.warn("❌ 유효하지 않은 JWT 토큰: path={}", path);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // 1. 인증/인가, 로그인 등 공개 API
        return path.startsWith("/oauth2/")
            || path.startsWith("/auth/")
            || path.startsWith("/login/")

            // 2. 공통 리소스(정적 파일, 헬스체크)
            || path.equals("/favicon.ico")
            || path.startsWith("/static/")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/")
            || path.equals("/actuator/health")

            // 3. Swagger, API 문서 리소스
            || path.startsWith("/swagger-ui/")
            || path.startsWith("/swagger-resources/")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/v2/api-docs")
            || path.startsWith("/webjars/");
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String token = null;

        // 1. Authorization 헤더에서 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
        }

        // 2. 토큰이 없으면 쿠키에서 추출
        if (token == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("access_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        return token;
    }

    private UsernamePasswordAuthenticationToken getAuthenticationToken(String token) {
        Long userId = jwtProvider.getUserId(token);
        UserDetails userDetails = customUserDetailService.loadUserById(userId);
        return new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
    }

}
