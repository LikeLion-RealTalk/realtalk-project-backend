package com.likelion.realtalk.domain.debate.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.dto.RoomUserInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisRoomTracker {

    private static final String ROOM_KEY_PREFIX = "debateRoom:";
    private static final String PARTICIPANTS_SUFFIX = ":waitingUsers";
    private static final String SPEAKERS = ":speakers";
    private static final String AUDIENCES = ":audiences";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String participantsKey(Long pk) {
        return ROOM_KEY_PREFIX + pk + PARTICIPANTS_SUFFIX;
    }
    private String speakersKey(Long pk)    { return ROOM_KEY_PREFIX + pk + SPEAKERS; }
    private String audiencesKey(Long pk)   { return ROOM_KEY_PREFIX + pk + AUDIENCES; }

    private Long safeLong(Object v) {
        if (v == null) return null;
        try { return Long.valueOf(String.valueOf(v)); }
        catch (NumberFormatException e) { return null; }
    }

    private boolean safeBool(Object v) {
        if (v instanceof Boolean b) return b;
        return v != null && Boolean.parseBoolean(String.valueOf(v));
    }

    /** 원자적 입장 (역할별 정원 체크 후 SADD + 상세 Hash 저장) - 세션 기준 */
    private final DefaultRedisScript<Long> tryEnterLua = new DefaultRedisScript<>(
        // KEYS[1] = roleSetKey, KEYS[2] = participantsHashKey
        // ARGV[1] = max, ARGV[2] = sessionId, ARGV[3] = json
        "local cnt = redis.call('SCARD', KEYS[1]) " +
        "if cnt >= tonumber(ARGV[1]) then return 0 end " +
        "redis.call('SADD', KEYS[1], ARGV[2]) " +
        "redis.call('HSET', KEYS[2], ARGV[2], ARGV[3]) " +
        "return 1"
    , Long.class);

    /** 입장: 세션 단위 */
    public boolean tryEnter(Long pk,
                            String role,
                            String sessionId,
                            int max,
                            String subjectId,     // "user:{userId}" or "guest:{sessionId}"
                            Long userId,          // 게스트면 null
                            String userName,      // 표시명
                            String side,
                            boolean authenticated) {

        String roleSet = "SPEAKER".equals(role) ? speakersKey(pk) : audiencesKey(pk);
        String pKey = participantsKey(pk);

        Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("subjectId", subjectId);
        userInfo.put("userId", userId); // null OK (게스트)
        userInfo.put("userName", userName);
        userInfo.put("role", role);
        userInfo.put("side", side);
        userInfo.put("authenticated", authenticated);
        userInfo.put("joinedAt", System.currentTimeMillis() / 1000L);

        String json;
        try { json = objectMapper.writeValueAsString(userInfo); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }

        Long ok = redisTemplate.execute(
            tryEnterLua,
            List.of(roleSet, pKey),
            String.valueOf(max), sessionId, json
        );
        return ok != null && ok == 1L;
    }

    /** 퇴장(역할 세트/해시에서 제거) - 역할 알고 있으면 */
    public void leave(Long pk, String role, String sessionId) {
        String roleSet = "SPEAKER".equals(role) ? speakersKey(pk) : audiencesKey(pk);
        redisTemplate.opsForSet().remove(roleSet, sessionId);
        redisTemplate.opsForHash().delete(participantsKey(pk), sessionId);
    }

    /** 퇴장(역할 모를 때 세션만으로 정리) */
    public void removeSession(Long pk, String sessionId) {
        redisTemplate.opsForSet().remove(speakersKey(pk), sessionId);
        redisTemplate.opsForSet().remove(audiencesKey(pk), sessionId);
        redisTemplate.opsForHash().delete(participantsKey(pk), sessionId);
    }

    public long getCurrentSpeakers(Long pk)  { return size(speakersKey(pk)); }
    public long getCurrentAudiences(Long pk) { return size(audiencesKey(pk)); }
    private long size(String key) {
        Long n = redisTemplate.opsForSet().size(key);
        return n == null ? 0L : n;
    }

    /** 참가자 상세 조회 (세션 기준) */
    public Map<String, RoomUserInfo> getRoomUserInfosByPk(Long pk) {
        String key = participantsKey(pk);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        Map<String, RoomUserInfo> out = new HashMap<>();
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            String sessionId = String.valueOf(e.getKey());
            String json = String.valueOf(e.getValue());
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(json, Map.class);

                RoomUserInfo info = RoomUserInfo.builder()
                    .sessionId(sessionId)
                    .subjectId((String) data.get("subjectId"))
                    .userId(safeLong(data.get("userId")))             // ← 숫자 아니면 null
                    .userName((String) data.get("userName"))
                    .role(String.valueOf(data.get("role")))
                    .side(String.valueOf(data.get("side")))
                    .authenticated(safeBool(data.get("authenticated")))
                    .build();

                out.put(sessionId, info);
            } catch (JsonProcessingException ignore) {
                // malformed entry skip
            }
        }
        return out;
    }

    public Set<Long> getAllRoomPks() {
        Set<String> keys = redisTemplate.keys(ROOM_KEY_PREFIX + "*" + PARTICIPANTS_SUFFIX);
        if (keys == null || keys.isEmpty()) return Set.of();
        return keys.stream().map(this::extractPkFromKey).filter(pk -> pk != null).collect(Collectors.toSet());
    }

    private Long extractPkFromKey(String key) {
        // "debateRoom:123:waitingUsers"
        try {
            int a = key.indexOf(':');
            int b = key.lastIndexOf(':');
            return Long.parseLong(key.substring(a + 1, b));
        } catch (Exception e) { return null; }
    }

    /** 참가자 상세 단건 조회 (세션 기준) */
    public RoomUserInfo findBySession(Long pk, String sessionId) {
        String key = participantsKey(pk);
        Object v = redisTemplate.opsForHash().get(key, sessionId);
        if (v == null) return null;

        String json = String.valueOf(v);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            return RoomUserInfo.builder()
                .sessionId(sessionId)
                .subjectId((String) data.get("subjectId"))
                .userId(safeLong(data.get("userId")))
                .userName((String) data.get("userName"))
                .role(String.valueOf(data.get("role")))
                .side(String.valueOf(data.get("side")))
                .authenticated(safeBool(data.get("authenticated")))
                .build();
        } catch (JsonProcessingException e) {
            return null; // malformed
        }
    }

    /** 참가자 전체 리스트 (UI 브로드캐스트 등 용도) */
    public List<RoomUserInfo> getParticipantsAsList(Long pk) {
        return getRoomUserInfosByPk(pk)
            .values()
            .stream()
            .collect(Collectors.toList());
    }
}
