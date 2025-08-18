package com.likelion.realtalk.domain.debate.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomCleanupService {

    private final StringRedisTemplate redis;
    private final RoomIdMappingService mapping; // UUID→PK 매핑 주입

    // ── 키 빌더(필요시 추가) ───────────────────────────────────────────────
    private String kWaiting(Long pk)   { return "debateRoom:" + pk + ":waitingUsers"; } // HASH
    private String kAudiences(Long pk) { return "debateRoom:" + pk + ":audiences"; }    // SET
    private String kSpeakers(Long pk)  { return "debateRoom:" + pk + ":speakers"; }     // SET (있으면)
    // private String kSessions(Long pk)  { return "debateRoom:" + pk + ":sessions"; }  // 쓰면 추가

    /** 토론 종료 시: 참가자 관련 키만 삭제 (매핑은 유지) — PK 버전 */
    public void cleanupParticipants(Long pk) {
        var keys = List.of(
            kWaiting(pk),
            kAudiences(pk),
            kSpeakers(pk)
            // , kSessions(pk)
        );
        redis.delete(keys); // 동기 삭제(DEL). 대량이면 UNLINK 사용 고려
    }

    /** 토론 종료 시: 참가자 관련 키만 삭제 (매핑은 유지) — UUID 버전 */
    public void cleanupParticipants(UUID uuid) {
        // 매핑이 없으면 조용히 종료 (필요시 로그만 남기기)
        Long pk;
        try {
            pk = mapping.toPk(uuid);
        } catch (IllegalArgumentException e) {
            log.warn("cleanupParticipants: 매핑 없음 uuid={}", uuid);
            return;
        }
        cleanupParticipants(pk);
    }

}
