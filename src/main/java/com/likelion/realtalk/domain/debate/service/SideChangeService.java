package com.likelion.realtalk.domain.debate.service;

import java.util.Map;
import java.util.Objects;
import java.util.Collection;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.realtalk.domain.debate.dto.RoomUserInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SideChangeService {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  // 이미 사용 중인 브로드캐스트/참가자 보관체계
  private final ParticipantService participantService; // roomParticipants 메모리 맵 접근용
  private final SideStatsService sideStatsService;     // 게이지 브로드캐스트
  // private final DebateRoomBroadcaster broadcaster;     // 내부적으로 broadcastParticipantsSpeaker(pk) 제공한다고 가정
  // 위 broadcaster가 없다면 participantService.broadcastParticipantsSpeaker(pk) 직접 호출하셔도 됩니다.

  private String waitingKey(Long pk) {
    return "debateRoom:" + pk + ":waitingUsers"; // 예: debateRoom:2:waitingUsers
  }

  /**
   * subjectId 사용자의 side를 "A"/"B"로 변경하고, 참가자/통계 브로드캐스트 수행
   */
  public void changeSideAndBroadcast(Long pk, String subjectId, String newSide) {
    String side = normalizeSide(newSide); // "A"/"B"만 허용
    updateRedisWaitingUsers(pk, subjectId, side);   // Redis JSON 업데이트
    updateInMemoryParticipants(pk, subjectId, side); // 서버 메모리 객체 동기화(있다면)

    // 실시간 반영
    participantService.broadcastParticipantsSpeaker(pk); // 기존에 쓰시던 참가자 브로드캐스트
    participantService.broadcastAllRooms(); // 기존에 쓰시던 참가자 브로드캐스트
    sideStatsService.broadcast(pk);                      // 새로 만든 A/B 통계 브로드캐스트
  }

  private String normalizeSide(String s) {
    String v = (s == null) ? "" : s.trim().toUpperCase();
    if (!Objects.equals(v, "A") && !Objects.equals(v, "B")) {
      throw new IllegalArgumentException("side must be 'A' or 'B'");
    }
    return v;
  }

  /** Redis 해시(debateRoom:{pk}:waitingUsers)에서 subjectId 매칭 항목들의 side를 일괄 변경 */
  private void updateRedisWaitingUsers(Long pk, String subjectId, String side) {
    HashOperations<String, String, String> ops = redisTemplate.opsForHash();
    String key = waitingKey(pk);
    Map<String, String> all = ops.entries(key);
    if (all == null || all.isEmpty()) return;

    for (Map.Entry<String, String> e : all.entrySet()) {
      try {
        JsonNode n = objectMapper.readTree(e.getValue());
        String sub = n.path("subjectId").asText(null);
        if (subjectId != null && subjectId.equals(sub)) {
          if (n instanceof ObjectNode on) {
            on.put("side", side);
            ops.put(key, e.getKey(), objectMapper.writeValueAsString(on));
          }
        }
      } catch (Exception ignore) {}
    }
  }

  /** 서버 메모리 roomParticipants에도 반영(가지고 계신 구조에 맞게 보정) */
    private void updateInMemoryParticipants(Long pk, String subjectId, String side) {
    Collection<RoomUserInfo> users = participantService.getDetailedUsersInRoom(pk);
    if (users == null || users.isEmpty()) return;

    for (RoomUserInfo u : users) {
        if (u != null && subjectId.equals(u.getSubjectId())) {
        u.setSide(side); // 컬렉션이 map.values() 뷰라 바로 반영됨
        }
    }
    }
}
