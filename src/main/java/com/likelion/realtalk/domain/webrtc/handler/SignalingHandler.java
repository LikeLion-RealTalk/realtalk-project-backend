package com.likelion.realtalk.domain.webrtc.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@RequiredArgsConstructor
@Component
public class SignalingHandler extends TextWebSocketHandler {

    // roomId -> Set<SessionInfo>
    private final Map<String, Set<SessionInfo>> rooms = new ConcurrentHashMap<>();

    // sessionId -> SessionInfo
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 세션 정보를 담는 내부 클래스
    private static class SessionInfo {
        private final WebSocketSession session;
        private final String userId;
        private String roomId;

        public SessionInfo(WebSocketSession session, String userId) {
            this.session = session;
            this.userId = userId;
        }

        // getters and setters
        public WebSocketSession getSession() { return session; }
        public String getUserId() { return userId; }
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("WebSocket 연결 수립: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode data = objectMapper.readTree(message.getPayload());
            String messageType = data.get("type").asText();

            System.out.println("수신된 메시지: " + messageType + " from " + session.getId());

            switch (messageType) {
                case "join-room":
                    handleJoinRoom(session, data);
                    break;
                case "offer":
                case "answer":
                case "ice-candidate":
                    handleWebRTCSignaling(session, data);
                    break;
                case "leave-room":
                    handleLeaveRoom(session);
                    break;
                default:
                    System.out.println("알 수 없는 메시지 타입: " + messageType);
            }
        } catch (Exception e) {
            System.err.println("메시지 처리 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 방 입장 처리
     */
    private void handleJoinRoom(WebSocketSession session, JsonNode data) throws IOException {
        String roomId = data.get("roomId").asText();
        String userId = data.get("userId").asText();

        System.out.println("방 입장 요청 - roomId: " + roomId + ", userId: " + userId);

        // 세션 정보 생성 및 저장
        SessionInfo sessionInfo = new SessionInfo(session, userId);
        sessionInfo.setRoomId(roomId);
        sessionMap.put(session.getId(), sessionInfo);

        // 방에 추가
        rooms.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(sessionInfo);

        // 기존 참가자들에게 새 사용자 입장 알림
        broadcastToRoom(roomId, createMessage("user-joined",
            Map.of("userId", userId)), session);

        // 새 사용자에게 기존 참가자 목록 전송
        Set<SessionInfo> roomParticipants = rooms.get(roomId);
        List<String> existingUsers = roomParticipants.stream()
            .filter(si -> !si.getUserId().equals(userId))
            .map(SessionInfo::getUserId)
            .toList();

        sendToSession(session, createMessage("room-users",
            Map.of("users", existingUsers)));

        // 방 정보 업데이트 (모든 참가자에게)
        broadcastToRoom(roomId, createMessage("room-info",
            Map.of("participantCount", roomParticipants.size())), null);

        System.out.println("사용자 " + userId + "가 방 " + roomId + "에 입장했습니다. (총 " + roomParticipants.size() + "명)");
    }

    /**
     * WebRTC 시그널링 처리 (offer, answer, ice-candidate)
     */
    private void handleWebRTCSignaling(WebSocketSession session, JsonNode data) throws IOException {
        SessionInfo senderInfo = sessionMap.get(session.getId());
        if (senderInfo == null || senderInfo.getRoomId() == null) {
            System.err.println("방에 입장하지 않은 세션에서 WebRTC 시그널링 시도");
            return;
        }

        String roomId = senderInfo.getRoomId();
        String messageType = data.get("type").asText();

        // targetUserId가 있는 경우 해당 사용자에게만 전송
        if (data.has("targetUserId")) {
            String targetUserId = data.get("targetUserId").asText();
            sendToUserInRoom(roomId, targetUserId, enhanceMessage(data, senderInfo.getUserId()));
        } else {
            // targetUserId가 없는 경우 방의 다른 모든 사용자에게 브로드캐스트
            broadcastToRoom(roomId, enhanceMessage(data, senderInfo.getUserId()), session);
        }

        System.out.println("WebRTC " + messageType + " 메시지 중계 완료 - 방: " + roomId);
    }

    /**
     * 방 떠나기 처리
     */
    private void handleLeaveRoom(WebSocketSession session) throws IOException {
        SessionInfo sessionInfo = sessionMap.get(session.getId());
        if (sessionInfo != null && sessionInfo.getRoomId() != null) {
            String roomId = sessionInfo.getRoomId();
            String userId = sessionInfo.getUserId();

            // 방에서 제거
            Set<SessionInfo> roomParticipants = rooms.get(roomId);
            if (roomParticipants != null) {
                roomParticipants.remove(sessionInfo);

                // 다른 참가자들에게 알림
                broadcastToRoom(roomId, createMessage("user-left",
                    Map.of("userId", userId)), null);

                // 방 정보 업데이트
                if (!roomParticipants.isEmpty()) {
                    broadcastToRoom(roomId, createMessage("room-info",
                        Map.of("participantCount", roomParticipants.size())), null);
                } else {
                    // 빈 방 제거
                    rooms.remove(roomId);
                    System.out.println("빈 방 " + roomId + " 제거됨");
                }
            }

            // 세션 정보 제거
            sessionMap.remove(session.getId());

            System.out.println("사용자 " + userId + "가 방 " + roomId + "에서 나갔습니다.");
        }
    }

    /**
     * 특정 방의 모든 사용자에게 메시지 브로드캐스트
     */
    private void broadcastToRoom(String roomId, String message, WebSocketSession excludeSession) {
        Set<SessionInfo> roomParticipants = rooms.get(roomId);
        if (roomParticipants == null) return;

        for (SessionInfo sessionInfo : roomParticipants) {
            WebSocketSession session = sessionInfo.getSession();
            if (session.isOpen() && !session.equals(excludeSession)) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    System.err.println("메시지 전송 실패: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 특정 방의 특정 사용자에게 메시지 전송
     */
    private void sendToUserInRoom(String roomId, String targetUserId, String message) {
        Set<SessionInfo> roomParticipants = rooms.get(roomId);
        if (roomParticipants == null) return;

        for (SessionInfo sessionInfo : roomParticipants) {
            if (sessionInfo.getUserId().equals(targetUserId) && sessionInfo.getSession().isOpen()) {
                try {
                    sessionInfo.getSession().sendMessage(new TextMessage(message));
                    break;
                } catch (IOException e) {
                    System.err.println("타겟 사용자에게 메시지 전송 실패: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 특정 세션에 메시지 전송
     */
    private void sendToSession(WebSocketSession session, String message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                System.err.println("세션에 메시지 전송 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 메시지 생성 헬퍼
     */
    private String createMessage(String type, Map<String, Object> data) throws IOException {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.putAll(data);
        return objectMapper.writeValueAsString(message);
    }

    /**
     * 기존 메시지에 발신자 정보 추가
     */
    private String enhanceMessage(JsonNode originalData, String fromUserId) throws IOException {
        Map<String, Object> enhancedMessage = objectMapper.convertValue(originalData, Map.class);
        enhancedMessage.put("fromUserId", fromUserId);
        return objectMapper.writeValueAsString(enhancedMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            handleLeaveRoom(session);
        } catch (IOException e) {
            System.err.println("연결 종료 처리 중 오류: " + e.getMessage());
        }
        System.out.println("WebSocket 연결 종료: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("WebSocket 전송 오류: " + exception.getMessage());
        try {
            handleLeaveRoom(session);
        } catch (IOException e) {
            System.err.println("오류 처리 중 추가 오류: " + e.getMessage());
        }
    }

    /**
     * 디버깅을 위한 방 상태 출력
     */
    public void printRoomStatus() {
        System.out.println("=== 현재 방 상태 ===");
        for (Map.Entry<String, Set<SessionInfo>> entry : rooms.entrySet()) {
            String roomId = entry.getKey();
            Set<SessionInfo> participants = entry.getValue();
            System.out.println("방 " + roomId + ": " + participants.size() + "명");
            for (SessionInfo participant : participants) {
                System.out.println("  - " + participant.getUserId());
            }
        }
        System.out.println("==================");
    }
}
