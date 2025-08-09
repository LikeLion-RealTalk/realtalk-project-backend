package com.likelion.realtalk.debate.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.debate.dto.RoomUserInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisRoomTracker {

    private static final String WAITING_ROOM_KEY_PREFIX = "debateRoom:";
    private static final String WAITING_USERS_KEY_SUFFIX = ":waitingUsers";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String waitingKey(Long pk){
        return WAITING_ROOM_KEY_PREFIX + pk + WAITING_USERS_KEY_SUFFIX;
    }

    public void userJoinedByPk(Long pk, String userId, String role, String side) {
        String key = WAITING_ROOM_KEY_PREFIX + pk + WAITING_USERS_KEY_SUFFIX;

        Map<String, String> userInfo = Map.of(
            "userId", userId,     // ✅ 명시적으로 JSON 내부에 포함
            "role", role,
            "side", side
        );

        try {
            String json = objectMapper.writeValueAsString(userInfo);
            redisTemplate.opsForHash().put(key, userId, json); // Redis key는 그대로 userId
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 저장용 JSON 직렬화 실패", e);
        }
    }

    public void userLeftByPk(Long pk, String userId) {
        String key = waitingKey(pk);
        redisTemplate.opsForHash().delete(key, userId);
    }

    public Set<String> getWaitingUsers(Long pk) {
        String key = waitingKey(pk);
        return redisTemplate.opsForHash().keys(key).stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    public long getWaitingUserCountByPk(Long pk) {
        String key = waitingKey(pk);
        Long count = redisTemplate.opsForHash().size(key);
        return count != null ? count : 0;
    }

    public long getCurrentSpeakers(Long pk) {
        return countByRole(pk, "SPEAKER");
    }

    public long getCurrentAudiences(Long pk) {
        return countByRole(pk, "AUDIENCE");
    }

    private long countByRole(Long pk, String role) {
        String key = waitingKey(pk);
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

    public Map<String, Map<String, String>> getWaitingUserInfos(UUID roomId) {
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

    public Set<Long> getAllRoomPks() {
        Set<String> keys = redisTemplate.keys(WAITING_ROOM_KEY_PREFIX + "*"+ WAITING_USERS_KEY_SUFFIX);
        if (keys == null || keys.isEmpty()) return Set.of();

        return keys.stream()
                .map(this::extractPkFromKey)
                .filter(pk -> pk != null)
                .collect(Collectors.toSet());
    }

    public Map<String, RoomUserInfo> getRoomUserInfosByPk(Long pk) {
        String key = waitingKey(pk);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        Map<String, RoomUserInfo> userInfos = new HashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String userId = entry.getKey().toString();
            String json = entry.getValue().toString();
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> data = objectMapper.readValue(json, Map.class);

                RoomUserInfo userInfo = RoomUserInfo.builder()
                    .userId(userId) // ✅ 여기서 직접 넣어줌
                    .role(data.get("role"))
                    .side(data.get("side"))
                    .build();

                userInfos.put(userId, userInfo);

            } catch (JsonProcessingException e) {
                // 예외 처리
            }
        }

        return userInfos;
    }

    private Long extractPkFromKey(String key) {
        // key 예: "room:123:waitingUsers"
        try {
            int a = key.indexOf(':');              // after "room"
            int b = key.lastIndexOf(':');          // before "waitingUsers"
            String num = key.substring(a + 1, b);  // "123"
            return Long.parseLong(num);
        } catch (Exception e) {
            return null;
        }
    }

}
