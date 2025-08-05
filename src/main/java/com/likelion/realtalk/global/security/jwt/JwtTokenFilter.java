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
        String token = getTokenFromRequest(request);

        if (token != null && jwtProvider.validateToken(token)) {
            UsernamePasswordAuthenticationToken authenticationToken = getAuthenticationToken(token);
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            String url = request.getRequestURI().toString();
            String method = request.getMethod();

            log.info("HTTP Request: {} {}", method, url);
            log.info("✅ Authenticated user: {}", authenticationToken.getName());
        } else {
            log.warn("❌ Invalid or missing JWT token");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/oauth2/") ||
            path.startsWith("/auth/") ||
            path.startsWith("/login/") ||
            path.startsWith("/user/check-username");
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
