package com.likelion.realtalk.debate.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.likelion.realtalk.debate.entity.DebateRoom;
import com.likelion.realtalk.debate.entity.DebateRoomStatus;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.likelion.realtalk.debate.repository.DebateRoomRepository;
import com.likelion.realtalk.debate.dto.RoomUserInfo;

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

    //사용자 추가
    // public void addUserToRoom(Long roomId, String userId, String sessionId) {
    public void addUserToRoom(Long roomId, String userId, String sessionId, String role, String side) {
        RoomUserInfo userInfo = RoomUserInfo.builder()
                .userId(userId)
                .role(role)
                .side(side)
                .build();

        // 사용자 등록
        roomParticipants
                .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(sessionId, userInfo);

        // redis에도 추가 
        redisRoomTracker.userJoined(roomId, userId, role, side);

        //상태 확인 및 시작 조건 체크
        handleRoomStartCheck(roomId);

        // 브로드캐스트
        broadcastParticipants(roomId);
        broadcastAllRooms();
    }

    // 토론 시작 조건
    private void handleRoomStartCheck(Long roomId) {
        DebateRoom room = debateRoomRepository.findById(roomId).orElse(null);

        if (room != null && room.getStatus() == DebateRoomStatus.waiting) {
            long userCount = redisRoomTracker.getWaitingUserCount(roomId);
            if (userCount >= room.getMaxSpeaker()) {
                room.setStatus(DebateRoomStatus.started);
                debateRoomRepository.save(room);
                debateEventPublisher.publishDebateStart(room);
            }
        }
    }

     //세션 ID로 사용자 제거 (브라우저 종료 등)
    public void removeUserBySession(String sessionId) {
        for (Long roomId : roomParticipants.keySet()) {
            Map<String, RoomUserInfo> sessionMap = roomParticipants.get(roomId);
            if (sessionMap != null && sessionMap.containsKey(sessionId)) {
                RoomUserInfo removedUser = sessionMap.remove(sessionId);

                if( removedUser != null) {
                    redisRoomTracker.userLeft(roomId, removedUser.getUserId());
                }

                broadcastParticipants(roomId);
                broadcastAllRooms();
                break;
            }
        }
    }

    // 특정 방에서 userId로 강제 제거 (직접 나가기 버튼 눌렀을 경우 등)
    public void removeUserFromRoom(Long roomId, String userId) {
        Map<String, RoomUserInfo> sessionMap = roomParticipants.get(roomId);
        if (sessionMap != null) {
            sessionMap.entrySet().removeIf(entry -> {
                boolean match = entry.getValue().getUserId().equals(userId);
                if (match) {
                    redisRoomTracker.userLeft(roomId, userId);
                }
                return match;
            });

            broadcastParticipants(roomId);
            broadcastAllRooms();
        }
    }

    public List<RoomUserInfo> getUserInfosInRoom(Long roomId) {
        return new ArrayList<>(roomParticipants.getOrDefault(roomId, Collections.emptyMap()).values());
    }
    // 현재 방의 유저 목록 반환
    // public List<String> getUsersInRoom(Long roomId) {
    //     return new ArrayList<>(roomParticipants.getOrDefault(roomId, Collections.emptyMap()).values());
    // }
    public List<String> getUsersInRoom(Long roomId) {
        return roomParticipants.getOrDefault(roomId, Collections.emptyMap())
                .values()
                .stream()
                .map(RoomUserInfo::getUserId)
                .toList();
    }

    // 브로드캐스트 (해당 방에 실시간 참여자 목록 전달)
    private void broadcastParticipants(Long roomId) {
        Collection<String> participants = getUsersInRoom(roomId);
        messagingTemplate.convertAndSend("/sub/debate-room/" + roomId + "/participants", participants);
    }

    private void broadcastAllRooms() {
        Map<Long, Collection<RoomUserInfo>> allRooms = new HashMap<>();

        for (Long roomId : roomParticipants.keySet()) {
            Collection<RoomUserInfo> users = roomParticipants.get(roomId).values();
            allRooms.put(roomId, users); // <-- 핵심: RoomUserInfo 객체 그대로 넣기
        }

        messagingTemplate.convertAndSend("/sub/debate-room/all/participants", allRooms);
    }
}
