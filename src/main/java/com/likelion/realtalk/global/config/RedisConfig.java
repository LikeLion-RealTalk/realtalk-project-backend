package com.likelion.realtalk.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis 설정에 대한 config class 입니다.
 *
 * @author : 오승훈
 * @fileName : RedisConfig
 * @since : 2025-08-01
 */
@Configuration
@EnableCaching
public class RedisConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Key: 문자열
    template.setKeySerializer(new StringRedisSerializer());
    // Value: JSON 직렬화
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

    // Hash 타입도 동일하게 직렬화
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
    RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
    redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
    return redisMessageListenerContainer;
  }
}