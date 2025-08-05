package com.likelion.realtalk.debate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    // Map<roomId, Map<sessionId, userId>>
    private final Map<Long, Map<String, String>> roomParticipants = new ConcurrentHashMap<>();

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 사용자 추가
     */
    public void addUserToRoom(Long roomId, String userId, String sessionId) {
        roomParticipants
                .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(sessionId, userId);

        broadcastParticipants(roomId);
    }

    /**
     * 세션 ID로 사용자 제거 (브라우저 종료 등)
     */
    public void removeUserBySession(String sessionId) {
        for (Long roomId : roomParticipants.keySet()) {
            Map<String, String> sessionMap = roomParticipants.get(roomId);
            if (sessionMap != null && sessionMap.containsKey(sessionId)) {
                sessionMap.remove(sessionId);
                broadcastParticipants(roomId);
                break;
            }
        }
    }

    /**
     * 특정 방에서 userId로 강제 제거 (직접 나가기 버튼 눌렀을 경우 등)
     */
    public void removeUserFromRoom(Long roomId, String userId) {
        Map<String, String> sessionMap = roomParticipants.get(roomId);
        if (sessionMap != null) {
            sessionMap.entrySet().removeIf(entry -> entry.getValue().equals(userId));
            broadcastParticipants(roomId);
        }
    }

    /**
     * 현재 방의 유저 목록 반환
     */
    public List<String> getUsersInRoom(Long roomId) {
        return new ArrayList<>(roomParticipants.getOrDefault(roomId, Collections.emptyMap()).values());
    }

    /**
     * 브로드캐스트 (해당 방에 실시간 참여자 목록 전달)
     */
    private void broadcastParticipants(Long roomId) {
        Collection<String> participants = getUsersInRoom(roomId);
        messagingTemplate.convertAndSend("/sub/debate-room/" + roomId + "/participants", participants);
    }
}
