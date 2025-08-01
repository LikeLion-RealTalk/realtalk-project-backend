package com.example.demo.service;

import com.example.demo.dto.CreateRoomRequest;
import com.example.demo.model.DebateRoom;
import com.example.demo.model.DebateRoomStatus;

import com.example.demo.repository.DebateRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebateService {

    private final DebateRoomRepository debateRoomRepository;
    private final RedisRoomTracker redisRoomTracker;
    private final DebateEventPublisher debateEventPublisher;

    public List<DebateRoom> findAllRooms() {
        System.out.printf("토론방 리스트: ", debateRoomRepository.findAll());
        return debateRoomRepository.findAll();
    }

    public DebateRoom findRoomById(Long id) {
        return debateRoomRepository.findById(id).orElse(null);
    }

    public DebateRoom createRoom(CreateRoomRequest request) {
        DebateRoom debateRoom = new DebateRoom();
        debateRoom.setCategoryId(request.getCategoryId());
        debateRoom.setUserId(request.getUserId());
        debateRoom.setTitle(request.getTitle());
        debateRoom.setDebateType(request.getDebateType());
        debateRoom.setStatus(DebateRoomStatus.waiting);
        debateRoom.setCreatedAt(LocalDateTime.now());
        debateRoom.setDurationSeconds(request.getDurationSeconds());
        debateRoom.setSideA(request.getSideA());
        debateRoom.setSideB(request.getSideB());
        debateRoom.setMaxParticipants(request.getMaxParticipants());
        return debateRoomRepository.save(debateRoom);
    }

    public void handleJoin(Long roomId, String userId) {
        redisRoomTracker.userJoined(roomId, userId);
        DebateRoom room = findRoomById(roomId);
        if (room != null && room.getStatus() == DebateRoomStatus.waiting) {
            long userCount = redisRoomTracker.getWaitingUserCount(roomId);
            if (userCount >= room.getMaxParticipants()) {
                room.setStatus(DebateRoomStatus.started);
                debateRoomRepository.save(room);
                debateEventPublisher.publishDebateStart(room);
            }
        }
    }

    public void handleLeave(Long roomId, String userId) {
        redisRoomTracker.userLeft(roomId, userId);
    }
}
