package com.likelion.realtalk.domain.debate.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.likelion.realtalk.domain.debate.dto.RoomUserInfo;
import com.likelion.realtalk.domain.debate.entity.DebateRoom;
import com.likelion.realtalk.domain.debate.entity.DebateRoomStatus;
import com.likelion.realtalk.domain.debate.repository.DebateRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    // Map<roomPk, Map<sessionId, RoomUserInfo>>
    private final Map<Long, Map<String, RoomUserInfo>> roomParticipants = new ConcurrentHashMap<>();

    private final SimpMessageSendingOperations messagingTemplate;
    private final RedisRoomTracker redisRoomTracker;
    private final DebateRoomRepository debateRoomRepository;
    private final DebateEventPublisher debateEventPublisher;
    private final RoomIdMappingService mapping;
    private final SideStatsService sideStatsService;     // 게이지 브로드캐스트

    /** 정원 검증 + 등록 (원자적) — 세션 기준 + Principal 정보 반영 */
    public boolean tryAddUserToRoomByPk(Long pk,
                                        String subjectId,     // "user:{userId}" | "guest:{sessionId}"
                                        String sessionId,
                                        String role,
                                        String side,
                                        String displayName,   // userName/게스트명
                                        boolean authenticated,
                                        Long userId           // 로그인 사용자 PK or null
    ) {
        var room = debateRoomRepository.findById(pk).orElse(null);
        if (room == null) return false;
        if (room.getStatus() == DebateRoomStatus.ended) return false;

        final boolean isSpeaker = "SPEAKER".equalsIgnoreCase(role);
        final Long maxSpeakerL  = room.getMaxSpeaker();
        final Long maxAudienceL = room.getMaxAudience();

        if (isSpeaker && maxSpeakerL == null) {
            log.error("[참가] 방 pk={} 의 maxSpeaker가 null 입니다.", pk);
            return false;
        }
        if (!isSpeaker && maxAudienceL == null) {
            log.error("[참가] 방 pk={} 의 maxAudience가 null 입니다.", pk);
            return false;
        }

        final int max = isSpeaker ? maxSpeakerL.intValue() : maxAudienceL.intValue();

        boolean ok = redisRoomTracker.tryEnter(
            pk, role, sessionId, max, subjectId, userId, displayName, side, authenticated
        );
        if (!ok) return false; // 정원 초과

        // 메모리 갱신 — 세션 기준
        RoomUserInfo userInfo = RoomUserInfo.builder()
                .sessionId(sessionId)
                .subjectId(subjectId)
                .userId(userId)
                .userName(displayName)
                .authenticated(authenticated)
                .role(role)
                .side(side)
                .build();

        roomParticipants.computeIfAbsent(pk, k -> new ConcurrentHashMap<>())
                        .put(sessionId, userInfo);

        // 시작 조건: 스피커 수만 확인
        handleRoomStartCheck(pk);

        // 브로드캐스트
        broadcastParticipantsSpeaker(pk);
        sideStatsService.sideStatsbroadcast(pk);                      // 새로 만든 A/B 통계 브로드캐스트
        // broadcastParticipants(pk);
        broadcastAllRooms();
        return true;
    }

    // 세션 ID로 사용자 제거 (브라우저 종료 등)
    public void removeUserBySession(String sessionId) {
        for (Long pk : roomParticipants.keySet()) {
            Map<String, RoomUserInfo> sessionMap = roomParticipants.get(pk);
            if (sessionMap != null && sessionMap.containsKey(sessionId)) {
                sessionMap.remove(sessionId);
                // Redis 정리 (역할 몰라도 세션 기준)
                redisRoomTracker.removeSession(pk, sessionId);

                broadcastParticipantsSpeaker(pk);
                sideStatsService.sideStatsbroadcast(pk);                      // 새로 만든 A/B 통계 브로드캐스트
                // broadcastParticipants(pk);
                broadcastAllRooms();
                break;
            }
        }
    }

    // 특정 방에서 주체(subjectId)로 강제 제거 (관리자 등)
    public void removeUserFromRoom(Long pk, String subjectId) {
        Map<String, RoomUserInfo> sessionMap = roomParticipants.get(pk);
        if (sessionMap != null) {
            sessionMap.entrySet().removeIf(entry -> {
                RoomUserInfo info = entry.getValue();
                // NPE 방지: subjectId 또는 info.getSubjectId()가 null이어도 안전
                boolean match = Objects.equals(subjectId, info != null ? info.getSubjectId() : null);
                if (match) {
                    redisRoomTracker.removeSession(pk, entry.getKey()); // sessionId
                }
                return match;
            });

            broadcastParticipantsSpeaker(pk);
            sideStatsService.sideStatsbroadcast(pk);                      // 새로 만든 A/B 통계 브로드캐스트
            // broadcastParticipants(pk);
            broadcastAllRooms();
        }
    }

    public List<RoomUserInfo> getUserInfosInRoom(Long pk) {
        return new ArrayList<>(roomParticipants.getOrDefault(pk, Collections.emptyMap()).values());
    }

    public Collection<RoomUserInfo> getDetailedUsersInRoom(Long pk) {
        return roomParticipants.getOrDefault(pk, Collections.emptyMap()).values();
    }

    // 브로드캐스트 (해당 방 참여자 목록 전달)
    public void broadcastParticipants(Long pk) {
        Collection<RoomUserInfo> sessions = roomParticipants.getOrDefault(pk, Collections.emptyMap()).values();
        // 동일 사용자 다중 세션을 합치고 싶으면 subjectId 기준으로 dedup
        Map<String, RoomUserInfo> dedup = new HashMap<>();
        for (RoomUserInfo u : sessions) {
            String key = (u.getSubjectId() != null) ? u.getSubjectId() : u.getSessionId();
            dedup.put(key, u);
        }
        UUID uuid = safeToUuid(pk);
        messagingTemplate.convertAndSend("/sub/debate-room/" + uuid + "/participants", dedup.values());
    }

    public void broadcastParticipantsSpeaker(Long pk) {
        Collection<RoomUserInfo> sessions =
            roomParticipants.getOrDefault(pk, Collections.emptyMap()).values();

        // 1) SPEAKER만 필터
        List<RoomUserInfo> speakers = sessions.stream()
            .filter(u -> u != null && u.getRole() != null
                && "SPEAKER".equalsIgnoreCase(u.getRole()))
            .toList();

        // 2) 동일 사용자 다중 세션 dedup (subjectId 우선, 없으면 sessionId)
        Map<String, RoomUserInfo> dedup = new LinkedHashMap<>();
        for (RoomUserInfo u : speakers) {
            String key = (u.getSubjectId() != null) ? u.getSubjectId() : u.getSessionId();
            if (key != null) dedup.put(key, u);
        }

        // 3) 발행
        UUID uuid = safeToUuid(pk);
        messagingTemplate.convertAndSend(
            "/sub/debate-room/" + uuid + "/participants",
            dedup.values()
        );
    }

    public void broadcastAllRooms() {
        Map<UUID, Collection<RoomUserInfo>> allRooms = new HashMap<>();
        for (Long pk : roomParticipants.keySet()) {
            UUID uuid = safeToUuid(pk);
            allRooms.put(uuid, roomParticipants.get(pk).values());
        }
        messagingTemplate.convertAndSend("/sub/debate-room/all/participants", allRooms);
    }

    private void handleRoomStartCheck(Long pk) {
        DebateRoom room = debateRoomRepository.findById(pk).orElse(null);
        if (room != null && room.getStatus() == DebateRoomStatus.waiting) {
            long speakerCount = redisRoomTracker.getCurrentSpeakers(pk);
            if (speakerCount >= room.getMaxSpeaker()) {
                //room.setStatus(DebateRoomStatus.started); => 발언자 수가 가득 찼을 때 자동 시작 비활성화를 위한 주석입니다.
                debateRoomRepository.save(room);
                debateEventPublisher.publishDebateStart(room);
            }
        }
    }

    private UUID safeToUuid(Long pk) {
        try {
            return mapping.toUuid(pk);
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(("missing-" + pk).getBytes());
        }
    }

    // 서버 시작 시 Redis -> 메모리 초기화
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        initRoomParticipantsFromRedis();
        broadcastAllRooms();
    }

    public void initRoomParticipantsFromRedis() {
        Set<Long> pks = redisRoomTracker.getAllRoomPks();
        for (Long pk : pks) {
            Map<String, RoomUserInfo> userMap = redisRoomTracker.getRoomUserInfosByPk(pk);
            if (userMap == null) userMap = Collections.emptyMap();

            // 게스트도 포함해야 하므로 userId null 허용, role/side 존재만 체크
            Map<String, RoomUserInfo> cleaned = new ConcurrentHashMap<>();
            for (Map.Entry<String, RoomUserInfo> e : userMap.entrySet()) {
                RoomUserInfo v = e.getValue();
                if (e.getKey() != null && v != null
                        && v.getRole() != null && v.getSide() != null) {
                    cleaned.put(e.getKey(), v);
                }
            }
            roomParticipants.put(pk, cleaned);
        }
    }
}
