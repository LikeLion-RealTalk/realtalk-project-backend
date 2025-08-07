package com.likelion.realtalk.domain.debate.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.dto.DebateMessageDto;
import com.likelion.realtalk.global.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class DebateRedisRepository {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  private static final String NORMAL_DEBATE_TYPE = "NORMAL";
  private static final String FAST_DEBATE_TYPE = "FAST";
  private static final Duration NORMAL_SPEAK_DURATION = Duration.ofMinutes(5);
  private static final Duration FAST_SPEAK_DURATION = Duration.ofSeconds(30);

  /* ======================= Value 저장 (TTL 포함) ======================= */
  public void putValueWithExpire(String key, String value, Duration duration) {
    redisTemplate.opsForValue().set(key, value, duration);
  }

  /* ======================= Domain 전용 메서드 ======================= */
  public void saveRoomField(String roomId, String key, String value) {
    putHashValue(RedisKeyUtil.getRoomKey(roomId), key, value);
  }

  public void saveParticipants(String roomId, Map<String, String> participantMap) {
    putJsonToHash(RedisKeyUtil.getRoomKey(roomId), "participants", participantMap);
  }

  public void saveSpokenUsers(String roomId, List<String> spokenUsers) {
    putJsonToHash(RedisKeyUtil.getRoomKey(roomId), "spokenUsers", spokenUsers);
  }

  public void saveSpeeches(String speechesKey, String turnNo, List<DebateMessageDto> speeches) {
    putJsonToHash(speechesKey, turnNo, speeches);
  }

  public void setExpireTime(String roomId, String expireKey) {
    Duration duration = getSpeakDuration(roomId);
    Instant expireTime = Instant.now().plus(duration);
    putValueWithExpire(expireKey, expireTime.toString(), duration);
  }

  public void expireTime(String roomId, String expireKey) {
      redisTemplate.opsForValue().set(
          expireKey,
        "trigger",
            Duration.ofMillis(1) // 1ms 뒤 만료
            );
  }


  public String getRoomField(String roomId, String key) {
    return getHashValue(RedisKeyUtil.getRoomKey(roomId), key);
  }

  public List<String> getParticipants(String roomId) {
    return readJsonFromHash(RedisKeyUtil.getRoomKey(roomId), "participants",
        new TypeReference<LinkedHashMap<String, String>>() {
        }).map(map -> new ArrayList<>(map.values())).orElseGet(ArrayList::new);
  }

  public List<String> getSpokenUsers(String roomId) {
    return readJsonFromHash(RedisKeyUtil.getRoomKey(roomId), "spokenUsers",
        new TypeReference<List<String>>() {
        }).orElseGet(ArrayList::new);
  }

  // 토론방 타입 별 발언 시간 반환
  private Duration getSpeakDuration(String roomId) {
    String type = getRoomField(roomId, "debateType");
    return NORMAL_DEBATE_TYPE.equals(type) ? NORMAL_SPEAK_DURATION : FAST_SPEAK_DURATION;
  }

  public List<DebateMessageDto> getSpeeches(String speechesKey, String turnNo) {
    return readJsonFromHash(speechesKey, turnNo, new TypeReference<List<DebateMessageDto>>() {
    }).orElseGet(ArrayList::new);
  }

  /* ======================= Generic JSON 저장/조회 ======================= */
  public <T> void putJsonToHash(String key, String hashKey, T value) {
    try {
      redisTemplate.opsForHash().put(key, hashKey, objectMapper.writeValueAsString(value));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Redis 직렬화 오류", e);
    }
  }

  public <T> Optional<T> readJsonFromHash(String key, String hashKey, TypeReference<T> typeRef) {
    String json = redisTemplate.<String, String>opsForHash().get(key, hashKey);
    if (json == null || json.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(json, typeRef));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Redis 역직렬화 오류", e);
    }
  }

  /* ======================= Hash 단일 값 저장/조회 ======================= */
  public void putHashValue(String key, String hashKey, String value) {
    redisTemplate.opsForHash().put(key, hashKey, value);
  }

  public String getHashValue(String key, String hashKey) {
    return redisTemplate.<String, String>opsForHash().get(key, hashKey);
  }

}
