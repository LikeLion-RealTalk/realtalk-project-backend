package com.likelion.realtalk.debate.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisRoomTracker {

    private static final String WAITING_ROOM_KEY_PREFIX = "debateRoom:";
    private static final String WAITING_USERS_KEY_SUFFIX = ":waitingUsers";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void userJoined(Long roomId, String userId, String role, String side) {
        String key = WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX;
        Map<String, String> userInfo = Map.of(
                "role", role,
                "side", side
        );
        try {
            String json = objectMapper.writeValueAsString(userInfo);
            redisTemplate.opsForHash().put(key, userId, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 저장용 JSON 직렬화 실패", e);
        }
    }

    public void userLeft(Long roomId, String userId) {
        String key = WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX;
        redisTemplate.opsForHash().delete(key, userId);
    }

    public Set<String> getWaitingUsers(Long roomId) {
        String key = WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX;
        return redisTemplate.opsForHash().keys(key).stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    public long getWaitingUserCount(Long roomId) {
        String key = WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX;
        Long count = redisTemplate.opsForHash().size(key);
        return count != null ? count : 0;
    }

    public long getCurrentSpeakers(Long roomId) {
        return countByRole(roomId, "SPEAKER");
    }

    public long getCurrentAudiences(Long roomId) {
        return countByRole(roomId, "AUDIENCE");
    }

    private long countByRole(Long roomId, String role) {
        String key = WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return entries.values().stream().filter(value -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> parsed = objectMapper.readValue(value.toString(), Map.class);
                return role.equals(parsed.get("role"));
            } catch (JsonProcessingException e) {
                return false;
            }
        }).count();
    }

    public Map<String, Map<String, String>> getWaitingUserInfos(Long roomId) {
        String key = WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX;
        Map<Object, Object> rawEntries = redisTemplate.opsForHash().entries(key);
        Map<String, Map<String, String>> result = new HashMap<>();

        for (Map.Entry<Object, Object> entry : rawEntries.entrySet()) {
            String userId = entry.getKey().toString();
            String json = entry.getValue().toString();
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> parsed = objectMapper.readValue(json, Map.class);
                result.put(userId, parsed);
            } catch (JsonProcessingException e) {
                // skip malformed
            }
        }

        return result;
    }
}
