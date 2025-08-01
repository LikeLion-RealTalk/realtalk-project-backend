package com.example.demo.service;

import com.example.demo.model.DebateRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DebateStartEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final RedisRoomTracker redisRoomTracker;

    @EventListener
    public void handleDebateStart(DebateEventPublisher.DebateStartEvent event) {
        DebateRoom room = event.getRoom();
        Map<String, Object> message = new HashMap<>();
        message.put("type", "START");
        message.put("message", "토론을 시작합니다");
        message.put("participants", redisRoomTracker.getWaitingUsers(room.getRoomId()));
        messagingTemplate.convertAndSend("/sub/debate-room/" + room.getRoomId(), message);
    }
}
