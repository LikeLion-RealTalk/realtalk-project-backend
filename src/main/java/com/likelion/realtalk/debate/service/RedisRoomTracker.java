package com.likelion.realtalk.debate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisRoomTracker {

    private static final String WAITING_ROOM_KEY_PREFIX = "debateRoom:";
    private static final String WAITING_USERS_KEY_SUFFIX = ":waitingUsers";

    private final StringRedisTemplate redisTemplate;

    public void userJoined(Long roomId, String userId) {
        redisTemplate.opsForSet().add(WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX, userId);
    }

    public void userLeft(Long roomId, String userId) {
        redisTemplate.opsForSet().remove(WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX, userId);
    }

    public Set<String> getWaitingUsers(Long roomId) {
        return redisTemplate.opsForSet().members(WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX);
    }

    public long getWaitingUserCount(Long roomId) {
        Long size = redisTemplate.opsForSet().size(WAITING_ROOM_KEY_PREFIX + roomId + WAITING_USERS_KEY_SUFFIX);
        return size != null ? size : 0;
    }
}
