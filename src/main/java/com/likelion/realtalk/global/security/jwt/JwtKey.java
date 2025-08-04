package com.likelion.realtalk.global.security.jwt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtKey {

  @Value("${spring.jwt.secret}")
  private String secretKey;

  @Bean
  public SecretKey jwtSecretKey() {
    byte[] keyBytes = secretKey.getBytes();
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }

}
