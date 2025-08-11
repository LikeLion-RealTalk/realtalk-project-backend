package com.likelion.realtalk.debate.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    private static final String SPEAKERS = ":speakers";
    private static final String AUDIENCES = ":audiences";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String waitingKey(Long pk){
        return WAITING_ROOM_KEY_PREFIX + pk + WAITING_USERS_KEY_SUFFIX;
    }

    private String speakersKey(Long pk){
        return WAITING_ROOM_KEY_PREFIX + pk + SPEAKERS;
    }

    private String audiencesKey(Long pk){
        return WAITING_ROOM_KEY_PREFIX + pk + AUDIENCES;
    }

    /** 원자적 입장 (역할별 정원 체크 후 SADD + 상세 Hash 저장) */
    private final DefaultRedisScript<Long> tryEnterLua = new DefaultRedisScript<>(
        // KEYS[1] = roleSetKey, KEYS[2] = waitingHashKey
        // ARGV[1] = max, ARGV[2] = userId, ARGV[3] = json({role,side})
        "local cnt = redis.call('SCARD', KEYS[1]) " +
        "if cnt >= tonumber(ARGV[1]) then return 0 end " +
        "redis.call('SADD', KEYS[1], ARGV[2]) " +
        "redis.call('HSET', KEYS[2], ARGV[2], ARGV[3]) " +
        "return 1"
    , Long.class);

    public boolean tryEnter(Long pk, String role, String userId, int max, String side) {
        String roleSet = "SPEAKER".equals(role) ? speakersKey(pk) : audiencesKey(pk);
        String wKey = waitingKey(pk);

        Map<String, String> userInfo = Map.of("userId", userId, "role", role, "side", side);
        String json;
        try { json = objectMapper.writeValueAsString(userInfo); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }

        Long ok = redisTemplate.execute(
            tryEnterLua,
            List.of(roleSet, wKey),
            String.valueOf(max), userId, json
        );
        return ok != null && ok == 1L;
    }

    /** 퇴장(역할 세트/해시에서 제거) */
    public void leave(Long pk, String role, String userId) {
        String roleSet = "SPEAKER".equals(role) ? speakersKey(pk) : audiencesKey(pk);
        redisTemplate.opsForSet().remove(roleSet, userId);
        redisTemplate.opsForHash().delete(waitingKey(pk), userId);
    }

    public long getCurrentSpeakers(Long pk)   { return size(speakersKey(pk)); }
    public long getCurrentAudiences(Long pk)  { return size(audiencesKey(pk)); }
    private long size(String key) {
        Long n = redisTemplate.opsForSet().size(key);
        return n == null ? 0L : n;
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

    // public long getCurrentSpeakers(Long pk) {
    //     return countByRole(pk, "SPEAKER");
    // }
    //
    // public long getCurrentAudiences(Long pk) {
    //     return countByRole(pk, "AUDIENCE");
    // }

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
