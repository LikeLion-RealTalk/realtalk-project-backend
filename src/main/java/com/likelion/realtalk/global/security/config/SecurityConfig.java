package com.likelion.realtalk.global.security.config;


import com.likelion.realtalk.domain.oauth.handler.OAuth2LoginSuccessHandler;
import com.likelion.realtalk.domain.oauth.handler.OAuth2LogoutSuccessHandler;
import com.likelion.realtalk.domain.oauth.service.OAuth2UserService;
import com.likelion.realtalk.global.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final OAuth2UserService OAuth2UserService;
  private final OAuth2LogoutSuccessHandler oAuth2LogoutSuccessHandler;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**", "/oauth2/**").permitAll()
            .anyRequest().authenticated()
        )
        .exceptionHandling(e -> e
            .authenticationEntryPoint((request, response, authException) -> {
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            })
        )
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo.userService(OAuth2UserService))
            .successHandler(oAuth2LoginSuccessHandler)
        )
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .logoutSuccessHandler(oAuth2LogoutSuccessHandler)
            .permitAll()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}

