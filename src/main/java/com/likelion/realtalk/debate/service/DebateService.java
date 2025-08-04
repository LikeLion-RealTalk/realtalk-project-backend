package com.likelion.realtalk.debate.service;

import com.likelion.realtalk.debate.dto.CreateRoomRequest;
import com.likelion.realtalk.debate.entity.DebateRoom;
import com.likelion.realtalk.debate.entity.DebateRoomStatus;

import com.likelion.realtalk.debate.repository.DebateRoomRepository;
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
        // System.out.printf("토론방 리스트: ", debateRoomRepository.findAll());
        return debateRoomRepository.findAll();
    }

    public DebateRoom findRoomById(Long id) {
        return debateRoomRepository.findById(id).orElse(null);
    }

    public DebateRoom createRoom(CreateRoomRequest request) {
        DebateRoom debateRoom = new DebateRoom();

        debateRoom.setRoomId(request.getRoomId()); // 수동 설정 필요 없다면 생략
        debateRoom.setUserId(request.getUserId());
        debateRoom.setTitle(request.getTitle());
        debateRoom.setDebateDescription(request.getDebateDescription());

        // 카테고리 ID 추출
        if (request.getCategory() != null) {
            debateRoom.setCategoryId(request.getCategory().getId());
        }

        debateRoom.setSideA(request.getSideA());
        debateRoom.setSideB(request.getSideB());

        debateRoom.setDebateType(request.getDebateType());
        debateRoom.setDurationSeconds(request.getDurationSeconds());
        debateRoom.setMaxSpeaker((long) request.getMaxSpeaker()); // DTO는 int, Entity는 Long
        debateRoom.setMaxListeners((long) request.getMaxAudience());

        debateRoom.setStatus(DebateRoomStatus.waiting); // enum 값 명확히 지정
        debateRoom.setCreatedAt(LocalDateTime.now());

        // maxParticipants는 요청에 없으면 계산해서 넣어야 할 수도 있음
        debateRoom.setMaxParticipants(
            (long) (request.getMaxSpeaker() + request.getMaxAudience())
        );

        return debateRoomRepository.save(debateRoom);
    }

    public void handleJoin(Long roomId, String userId) {
        redisRoomTracker.userJoined(roomId, userId);
        DebateRoom room = findRoomById(roomId);
        if (room != null && room.getStatus() == DebateRoomStatus.waiting) {
            long userCount = redisRoomTracker.getWaitingUserCount(roomId);
            System.out.println("userCount:"+ userCount);
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
