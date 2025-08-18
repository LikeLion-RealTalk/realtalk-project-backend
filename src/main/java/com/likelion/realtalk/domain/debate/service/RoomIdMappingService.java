package com.likelion.realtalk.domain.debate.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomIdMappingService {
    private final StringRedisTemplate redis;

    // 내부 키 전략은 외부에 노출하지 않음
    private String keyUuid(UUID uuid) { return "room:uuid:" + uuid; } // UUID → PK
    private String keyPk(Long pk)     { return "room:pk:" + pk; }     // PK → UUID

    public void put(UUID uuid, Long pk) {
        redis.opsForValue().set(keyUuid(uuid), pk.toString());
        redis.opsForValue().set(keyPk(pk), uuid.toString());
    }

    public Long toPk(UUID uuid) {
        String v = redis.opsForValue().get(keyUuid(uuid));
        if (v == null) throw new IllegalArgumentException("UUID 매핑 없음: " + uuid);
        return Long.valueOf(v);
    }

    public UUID toUuid(Long pk) {
        String v = redis.opsForValue().get(keyPk(pk));
        if (v == null) throw new IllegalArgumentException("PK 매핑 없음: " + pk);
        return UUID.fromString(v);
    }

    /** PK 리스트를 한 번에 UUID로 변환 (MGET) */
    public Map<Long, UUID> toUuidBatch(List<Long> pks) {
        if (pks.isEmpty()) return Collections.emptyMap();

        List<String> keys = pks.stream().map(this::keyPk).toList();
        List<String> vals = redis.opsForValue().multiGet(keys);

        Map<Long, UUID> result = new HashMap<>();
        for (int i = 0; i < pks.size(); i++) {
            String v = vals.get(i);
            if (v != null) {
                result.put(pks.get(i), UUID.fromString(v));
            }
        }
        return result;
    }

}
