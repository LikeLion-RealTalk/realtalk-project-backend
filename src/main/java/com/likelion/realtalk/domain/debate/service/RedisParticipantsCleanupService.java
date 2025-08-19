package com.likelion.realtalk.domain.debate.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisParticipantsCleanupService {

    private final StringRedisTemplate redis;

    private static final List<String> PATTERNS = List.of(
        "debateRoom:*:waitingUsers",
        "debateRoom:*:audiences",
        "debateRoom:*:speakers",
        "debateRoom:*:sessions" // 사용 중일 때만 의미 있음
    );

    /** 전체 방의 참가자 관련 키를 모두 삭제 (비동기 UNLINK) */
    public long cleanupAllParticipantsKeys() {
        long total = 0;
        for (String pattern : PATTERNS) {
            total += unlinkByScan(pattern);
        }
        return total;
    }

    /** 특정 방(pk)의 참가자 관련 키만 삭제 */
    public long cleanupParticipantsByPk(Long pk) {
        List<String> keys = List.of(
            "debateRoom:" + pk + ":waitingUsers",
            "debateRoom:" + pk + ":audiences",
            "debateRoom:" + pk + ":speakers",
            "debateRoom:" + pk + ":sessions"
        );
        return redis.execute((RedisConnection conn) -> {
            byte[][] arr = keys.stream()
                .map(k -> k.getBytes(StandardCharsets.UTF_8))
                .toArray(byte[][]::new);
            // UNLINK(비동기) → 블로킹 줄임. 즉시 삭제 원하면 conn.del(arr);
            Long n = conn.unlink(arr);
            return n == null ? 0L : n;
        });
    }

    /** 패턴으로 SCAN하여 모은 뒤 UNLINK */
    private long unlinkByScan(String pattern) {
        List<byte[]> toDelete = new ArrayList<>(1024);

        Long deleted = redis.execute((RedisConnection conn) -> {
            ScanOptions opts = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

            try (Cursor<byte[]> cur = conn.scan(opts)) {
                while (cur.hasNext()) {
                    toDelete.add(cur.next());
                }
            } catch (Exception e) {
                throw new RuntimeException("SCAN failed for pattern " + pattern, e);
            }

            if (toDelete.isEmpty()) return 0L;

            byte[][] arr = toDelete.toArray(byte[][]::new);
            Long n = conn.unlink(arr); // 비동기 해제
            return n == null ? 0L : n;
        });

        return deleted == null ? 0L : deleted;
    }
}
