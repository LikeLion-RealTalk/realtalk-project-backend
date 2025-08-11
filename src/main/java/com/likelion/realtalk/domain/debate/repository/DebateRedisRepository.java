package com.likelion.realtalk.domain.debate.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.dto.AiSummaryDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import com.likelion.realtalk.global.redis.RedisKeyUtil;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
  public void saveRoomField(String roomUUID, String key, String value) {
    putHashValue(RedisKeyUtil.getRoomKey(roomUUID), key, value);
  }

  public void saveParticipants(String roomUUID, Map<String, String> participantMap) {
    putJsonToHash(RedisKeyUtil.getRoomKey(roomUUID), "participants", participantMap);
  }

  public void saveSpokenUsers(String roomUUID, List<String> spokenUsers) {
    putJsonToHash(RedisKeyUtil.getRoomKey(roomUUID), "spokenUsers", spokenUsers);
  }

  public void saveSpeeches(String speechesKey, String turnNo, List<SpeakerMessageDto> speeches) {
    putJsonToHash(speechesKey, turnNo, speeches);
  }

  public void saveAiSummaries(String aiSummariesKey, String turnNo, List<AiSummaryDto> summaries) {
    putJsonToHash(aiSummariesKey, turnNo, summaries);
  }

  public String setExpireTime(String roomUUID, String expireKey) {
    Duration duration = getSpeakDuration(roomUUID);
    Instant expireTime = Instant.now().plus(duration);

    // 한국 시간(KST)으로 변환 후 포맷
    ZoneId kstZone = ZoneId.of("Asia/Seoul" );
    String expireTimeKST = expireTime.atZone(kstZone)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss" ));

    putValueWithExpire(expireKey, expireTimeKST, duration);

    return expireTimeKST;
  }

  public void expireTime(String expireKey) {
    redisTemplate.opsForValue().set(expireKey, "trigger", Duration.ofMillis(1) // 1ms 뒤 만료
    );
  }

  public String getCurrentSpeakerExpire(String roomUUID) {
    return redisTemplate.opsForValue().get(RedisKeyUtil.getExpireKey(roomUUID));
  }

  public String getAudienceExpire(String roomUUID) {
    return redisTemplate.opsForValue().get(RedisKeyUtil.getAudienceExpireKey(roomUUID));
  }

  public String getRoomField(String roomUUID, String key) {
    return getHashValue(RedisKeyUtil.getRoomKey(roomUUID), key);
  }

  public List<String> getParticipants(String roomUUID) {
    return readJsonFromHash(RedisKeyUtil.getRoomKey(roomUUID), "participants",
        new TypeReference<LinkedHashMap<String, String>>() {
        }).map(map -> new ArrayList<>(map.values())).orElseGet(ArrayList::new);
  }

  public List<String> getSpokenUsers(String roomUUID) {
    return readJsonFromHash(RedisKeyUtil.getRoomKey(roomUUID), "spokenUsers",
        new TypeReference<List<String>>() {
        }).orElseGet(ArrayList::new);
  }

  // 토론방 타입 별 발언 시간 반환
  private Duration getSpeakDuration(String roomUUID) {
    String type = getRoomField(roomUUID, "debateType" );
    return NORMAL_DEBATE_TYPE.equals(type) ? NORMAL_SPEAK_DURATION : FAST_SPEAK_DURATION;
  }

  public List<SpeakerMessageDto> getSpeeches(String speechesKey, String turnNo) {
    return readJsonFromHash(speechesKey, turnNo, new TypeReference<List<SpeakerMessageDto>>() {
    }).orElseGet(ArrayList::new);
  }

  public List<AiSummaryDto> getAiSummaries(String aiSumarryKey, String turnNo) {
    return readJsonFromHash(aiSumarryKey, turnNo, new TypeReference<List<AiSummaryDto>>() {
    }).orElseGet(ArrayList::new);
  }

  public void deleteByKey(String key) {
    redisTemplate.delete(key);
  }

  /* ======================= Generic JSON 저장/조회 ======================= */
  public <T> void putJsonToHash(String key, String hashKey, T value) {
    if (hashKey == null) {
      throw new RuntimeException("해당 토롱방의 turn 정보가 없습니다." );
    }
    try {
      redisTemplate.opsForHash().put(key, hashKey, objectMapper.writeValueAsString(value));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Redis 직렬화 오류", e);
    }
  }

  public <T> Optional<T> readJsonFromHash(String key, String hashKey, TypeReference<T> typeRef) {
    if (hashKey == null) {
      throw new RuntimeException("해당 토론방의 turn 정보가 없습니다." );
    }
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
