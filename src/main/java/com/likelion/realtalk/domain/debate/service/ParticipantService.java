package com.likelion.realtalk.domain.debate.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Service
@RequiredArgsConstructor
public class ParticipantService {

    // Map<roomId, Map<sessionId, userId>>
    // private final Map<Long, Map<String, String>> roomParticipants = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, RoomUserInfo>> roomParticipants = new ConcurrentHashMap<>();

    private final SimpMessageSendingOperations messagingTemplate;
    
    private final RedisRoomTracker redisRoomTracker;
    private final DebateRoomRepository debateRoomRepository;
    private final DebateEventPublisher debateEventPublisher;

    private final RoomIdMappingService mapping;

    //사용자 추가
    // public void addUserToRoom(Long roomId, String userId, String sessionId) {
    public void addUserToRoomByPk(Long pk, String userId, String sessionId, String role, String side) {
        RoomUserInfo userInfo = RoomUserInfo.builder()
                .userId(userId)
                .role(role)
                .side(side)
                .build();

        // 사용자 등록
        roomParticipants
                .computeIfAbsent(pk, k -> new ConcurrentHashMap<>())
                .put(sessionId, userInfo);

        // redis에도 추가 
        redisRoomTracker.userJoinedByPk(pk, userId, role, side);

        //상태 확인 및 시작 조건 체크
        handleRoomStartCheck(pk);

        // 브로드캐스트
        broadcastParticipants(pk);
        broadcastAllRooms();
    }

    /** 정원 검증 + 등록 (원자적) */
    public boolean tryAddUserToRoomByPk(Long pk, String userId, String sessionId, String role, String side) {
        var room = debateRoomRepository.findById(pk).orElse(null);
        if (room == null) return false;

        if (room.getStatus() == DebateRoomStatus.ended) return false;

        int max = "SPEAKER".equals(role) ? room.getMaxSpeaker().intValue()
                                         : room.getMaxAudience().intValue();

        boolean ok = redisRoomTracker.tryEnter(pk, role, userId, max, side);
        if (!ok) return false; // 정원 초과

        // 메모리(세션 매핑) 갱신
        RoomUserInfo userInfo = RoomUserInfo.builder()
                .userId(userId).role(role).side(side).build();

        roomParticipants.computeIfAbsent(pk, k -> new ConcurrentHashMap<>())
                        .put(sessionId, userInfo);

        // 시작 조건: 스피커 수만 확인
        handleRoomStartCheck(pk);

        // 브로드캐스트
        broadcastParticipants(pk);
        broadcastAllRooms();
        return true;
    }

    // (이행기용) UUID를 직접 받는 진입점이 필요하면 이 오버로드 사용 가능
    public void addUserToRoom(UUID roomUuid, String userId, String sessionId, String role, String side) {
        Long pk = mapping.toPk(roomUuid);
        addUserToRoomByPk(pk, userId, sessionId, role, side);
    }

    // 토론 시작 조건
    private void handleRoomStartCheck(Long pk) {
        DebateRoom room = debateRoomRepository.findById(pk).orElse(null);

        if (room != null && room.getStatus() == DebateRoomStatus.waiting) {
            long speakerCount = redisRoomTracker.getCurrentSpeakers(pk); // ✅ 수정: 스피커만
            if (speakerCount >= room.getMaxSpeaker()) {
                room.setStatus(DebateRoomStatus.started);
                debateRoomRepository.save(room);
                debateEventPublisher.publishDebateStart(room);
            }
        }
    }

     //세션 ID로 사용자 제거 (브라우저 종료 등)
    public void removeUserBySession(String sessionId) {
        for (Long pk : roomParticipants.keySet()) {
            Map<String, RoomUserInfo> sessionMap = roomParticipants.get(pk);
            if (sessionMap != null && sessionMap.containsKey(sessionId)) {
                RoomUserInfo removedUser = sessionMap.remove(sessionId);

                if( removedUser != null) {
                    redisRoomTracker.leave(pk,removedUser.getRole() ,removedUser.getUserId());
                }

                broadcastParticipants(pk);
                broadcastAllRooms();
                break;
            }
        }
    }

    // 특정 방에서 userId로 강제 제거 (직접 나가기 버튼 눌렀을 경우 등)
    public void removeUserFromRoom(Long pk, String userId) {
        Map<String, RoomUserInfo> sessionMap = roomParticipants.get(pk);
        if (sessionMap != null) {
            sessionMap.entrySet().removeIf(entry -> {
                boolean match = userId.equals(entry.getValue().getUserId());
                if (match) {
                    redisRoomTracker.userLeftByPk(pk, userId);
                }
                return match;
            });

            broadcastParticipants(pk);
            broadcastAllRooms();
        }
    }

    public List<RoomUserInfo> getUserInfosInRoom(Long pk) {
        return new ArrayList<>(roomParticipants.getOrDefault(pk, Collections.emptyMap()).values());
    }

    // 현재 방의 유저 목록 반환
    // public List<String> getUsersInRoom(Long roomId) {
    //     return new ArrayList<>(roomParticipants.getOrDefault(roomId, Collections.emptyMap()).values());
    // }

    // public List<String> getUsersInRoom(Long roomId) {
    //     return roomParticipants.getOrDefault(roomId, Collections.emptyMap())
    //             .values()
    //             .stream()
    //             .map(RoomUserInfo::getUserId)
    //             .toList();
    // }

    public Collection<RoomUserInfo> getDetailedUsersInRoom(Long pk) {
        return roomParticipants.getOrDefault(pk, Collections.emptyMap()).values();
    }

    // 브로드캐스트 (해당 방에 실시간 참여자 목록 전달)
    public void broadcastParticipants(Long pk) {
        Collection<RoomUserInfo> sessions = roomParticipants.getOrDefault(pk, Collections.emptyMap()).values();
        Map<String, RoomUserInfo> dedup = new HashMap<>();
        for (RoomUserInfo u : sessions) dedup.put(u.getUserId(), u);
        UUID uuid = safeToUuid(pk);
        messagingTemplate.convertAndSend("/sub/debate-room/" + uuid + "/participants", dedup.values());
    }

    public void broadcastAllRooms() {
        Map<UUID, Collection<RoomUserInfo>> allRooms = new HashMap<>();

        for (Long pk : roomParticipants.keySet()) {
            UUID uuid = safeToUuid(pk);
            allRooms.put(uuid, roomParticipants.get(pk).values()); // <-- 핵심: RoomUserInfo 객체 그대로 넣기
        }

        messagingTemplate.convertAndSend("/sub/debate-room/all/participants", allRooms);
    }

    private UUID safeToUuid(Long pk) {
        try {
            return mapping.toUuid(pk);
        } catch (Exception e) {
            // 매핑이 없을 수도 있으니 방어
            return UUID.nameUUIDFromBytes(("missing-" + pk).getBytes());
        }
    }

    public void initRoomParticipantsFromRedis() {
        // Redis에 존재하는 모든 roomId를 가져온다 (예: debateRoom:{roomId}:waitingUsers)
        // Set<String> keys = redisRoomTracker.getAllRoomKeys(); // 예: debateRoom:1:waitingUsers 등

        Set<Long> pks = redisRoomTracker.getAllRoomPks();

        for (Long pk : pks) {
            Map<String, RoomUserInfo> userMap = redisRoomTracker.getRoomUserInfosByPk(pk);
            if (userMap == null) userMap = Collections.emptyMap();

            // UUID roomId = extractRoomIdFromKey(key); // 예: "debateRoom:1:waitingUsers" → 1

            // null entry 방어
            Map<String, RoomUserInfo> cleaned = new ConcurrentHashMap<>();
            for (Map.Entry<String, RoomUserInfo> e : userMap.entrySet()) {
                if (e.getKey() != null && e.getValue() != null
                        && e.getValue().getUserId() != null
                        && e.getValue().getRole()   != null
                        && e.getValue().getSide()   != null) {
                    cleaned.put(e.getKey(), e.getValue());
                }
            }
            roomParticipants.put(pk, cleaned);
        }
    }

    private UUID extractRoomIdFromKey(String key) {
        // key: "debateRoom:550e8400-e29b-41d4-a716-446655440000:waitingUsers"
        try {
            String[] parts = key.split(":");
            return UUID.fromString(parts[1]); // ← 여기서 변환
        } catch (Exception e) {
            return null;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        initRoomParticipantsFromRedis(); // 💡 Redis에서 초기화
        System.out.println("✅ 서버 시작됨 - 전체 참여자 목록 초기 브로드캐스트 실행");
        broadcastAllRooms();
    }

}
