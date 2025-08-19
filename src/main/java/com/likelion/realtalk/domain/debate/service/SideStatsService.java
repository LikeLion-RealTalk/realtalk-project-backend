package com.likelion.realtalk.domain.debate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.dto.SideStatsDto;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SideStatsService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RoomIdMappingService roomIdMappingService; // UUID<->PK 매핑 사용 중이시죠
    private final SimpMessageSendingOperations messagingTemplate;

    private String waitingKey(Long pk) {
        // 예: debateRoom:2:waitingUsers
        return "debateRoom:" + pk + ":waitingUsers";
    }

    /** Redis에서 현재 대기자(참여자) A/B 카운트 집계 */
    public SideStatsDto compute(Long pk) {
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> all = ops.entries(waitingKey(pk));

        long a = 0, b = 0;
        if (all != null) {
            for (String json : all.values()) {
                try {
                    JsonNode n = objectMapper.readTree(json);
                    String side = n.path("side").asText(""); // "A" or "B"
                    if ("A".equalsIgnoreCase(side)) a++;
                    else if ("B".equalsIgnoreCase(side)) b++;
                } catch (Exception ignore) {
                    // 값이 JSON이 아닐 수 있거나 필드 누락 시 스킵
                }
            }
        }
        long total = a + b;
        int pA = (total == 0) ? 0 : (int) Math.round((a * 100.0) / total);
        int pB = 100 - pA;

        return SideStatsDto.builder()
                .countA(a)
                .countB(b)
                .total(total)
                .percentA(pA)
                .percentB(pB)
                .tsEpochSec(Instant.now().getEpochSecond())
                .build();
    }

    /** STOMP 구독자에게 브로드캐스트 */
    public void broadcast(Long pk) {
        SideStatsDto dto = compute(pk);
        UUID uuid = roomIdMappingService.toUuid(pk);
        messagingTemplate.convertAndSend("/sub/debate-room/" + uuid + "/side-stats", dto);
    }
}
