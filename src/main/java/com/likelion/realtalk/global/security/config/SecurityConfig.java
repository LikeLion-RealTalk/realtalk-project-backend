package com.likelion.realtalk.global.security.config;


import com.likelion.realtalk.domain.oauth.handler.OAuth2LoginFailureHandler;
import com.likelion.realtalk.domain.oauth.handler.OAuth2LoginSuccessHandler;
import com.likelion.realtalk.domain.oauth.handler.OAuth2LogoutSuccessHandler;
import com.likelion.realtalk.domain.oauth.service.OAuth2UserService;
import com.likelion.realtalk.global.security.jwt.JwtTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenFilter jwtTokenFilter;
  private final OAuth2UserService OAuth2UserService;
  private final OAuth2LogoutSuccessHandler oAuth2LogoutSuccessHandler;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

  @Value("${frontend.url:https://www.realtalks.co.kr}")
  private String frontendUrl;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/auth/**", "/oauth2/**", "/login/**", "/health", "/actuator/**","/test/**", "/signal/**",
                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/ws/**","/ws-speech/**", "/websocket/**",
                "/favicon.ico", "/static/**", "/css/**", "/js/**", "/images/**","/*.html", "/api/categories/**", "/ws-stomp/**"
            ).permitAll()
            .anyRequest().permitAll() // 이따 바꾸기
        )
        .exceptionHandling(e -> e
            .authenticationEntryPoint(authenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler())
        )
        .oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(authorization -> authorization
                .baseUri("/oauth2/authorization")
            )
            .redirectionEndpoint(redirection -> redirection
                .baseUri("/login/oauth2/code/*")
            )
            .userInfoEndpoint(userInfo -> userInfo.userService(OAuth2UserService))
            .successHandler(oAuth2LoginSuccessHandler)
            .failureHandler(oAuth2LoginFailureHandler)
        )
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .logoutSuccessHandler(oAuth2LogoutSuccessHandler)
            .permitAll()
        )
        .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // CORS 설정
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        frontendUrl,
        "https://api.realtalks.co.kr:8443"
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  // 인증 실패시 JSON 응답 설정
  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\": 401, \"message\": \"" + authException.getMessage() + "\"}");
      response.getWriter().flush();
    };
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\": 403, \"message\": \"" + accessDeniedException.getMessage() + "\"}");
      response.getWriter().flush();
    };
  }
}
