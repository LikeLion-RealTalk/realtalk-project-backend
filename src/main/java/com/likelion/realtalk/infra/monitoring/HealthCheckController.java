package com.likelion.realtalk.infra.monitoring;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

  private final DataSource dataSource;
  private final RedisTemplate<String, Object> redisTemplate;

  @GetMapping
  public ResponseEntity<Map<String, Object>> healthCheck() {
    Map<String, Object> status = new HashMap<>();

    // DB 연결 확인
    try (Connection conn = dataSource.getConnection()) {
      status.put("db", "UP");
    } catch (Exception e) {
      status.put("db", "DOWN");
    }

    // Redis 연결 확인
    try {
      redisTemplate.opsForValue().get("health-check");
      status.put("redis", "UP");
    } catch (Exception e) {
      status.put("redis", "DOWN");
    }

    // 서버 상태
    status.put("status", "UP");
    return ResponseEntity.ok(status);
  }
}

