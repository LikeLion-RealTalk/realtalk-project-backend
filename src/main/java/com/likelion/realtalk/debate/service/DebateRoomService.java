package com.likelion.realtalk.debate.service;

import com.likelion.realtalk.debate.dto.AiSummaryResponse;
import com.likelion.realtalk.debate.dto.CreateRoomRequest;
import com.likelion.realtalk.debate.dto.DebateRoomResponse;
import com.likelion.realtalk.debate.entity.DebateRoom;
import com.likelion.realtalk.debate.entity.DebateRoomStatus;

import com.likelion.realtalk.debate.repository.DebateRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.time.Duration;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebateRoomService {

    private final DebateRoomRepository debateRoomRepository;
    private final RedisRoomTracker redisRoomTracker;
    // private final DebateEventPublisher debateEventPublisher;

    private Long calculateElapsedSeconds(LocalDateTime startedAt) {
        if (startedAt == null) return 0L;
        return Duration.between(startedAt, LocalDateTime.now()).getSeconds();
    }

    public DebateRoom findRoomSummaryById(Long id) {
        return debateRoomRepository.findById(id).orElse(null);
    }

    //AI 정리
    public AiSummaryResponse findAiSummaryById(Long roomId) {
        DebateRoom room = debateRoomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("AI 결과물을 찾을 수 없습니다."));

        return AiSummaryResponse.builder()
                .roomId(room.getRoomId())
                .title(room.getTitle())
                .category(AiSummaryResponse.CategoryDto.builder()
                        .id(room.getCategoryId())
                        .name("카테고리 이름은 추후 조회") // 카테고리 명 로직 추가 필요
                        .build())
                .summary(room.getAiSummary())
                .build();
    }

    //모든 토론방 조회
    public List<DebateRoomResponse> findAllRooms() {
        List<DebateRoom> rooms = debateRoomRepository.findAll();

        return rooms.stream().map((DebateRoom room) -> {
            Long currentSpeaker = redisRoomTracker.getCurrentSpeakers(room.getRoomId());
            Long currentAudience = redisRoomTracker.getCurrentAudiences(room.getRoomId());
            Long elapsedSeconds = calculateElapsedSeconds(room.getCreatedAt());

            return DebateRoomResponse.builder()
                    .roomId(room.getRoomId())
                    .title(room.getTitle())
                    .status(room.getStatus().name())
                    .category(DebateRoomResponse.CategoryDto.builder()
                            .id(room.getCategoryId())
                            .name("카테고리 이름은 추후 조회") // 카테고리 이름이 있다면 조회 로직 필요
                            .build())
                    .sideA(room.getSideA())
                    .sideB(room.getSideB())
                    .maxSpeaker(room.getMaxSpeaker())
                    .maxAudience(room.getMaxAudience())
                    .currentSpeaker(currentSpeaker)
                    .currentAudience(currentAudience)
                    .elapsedSeconds(elapsedSeconds)
                    .build();
        }).collect(Collectors.toList());
    }

    //roomId 토론방 검색
    public DebateRoomResponse findRoomById(Long roomId) {
        DebateRoom room = debateRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("토론방을 찾을 수 없습니다."));

        Long currentSpeaker = redisRoomTracker.getCurrentSpeakers(room.getRoomId());
        Long currentAudience = redisRoomTracker.getCurrentAudiences(room.getRoomId());
        Long elapsedSeconds = calculateElapsedSeconds(room.getCreatedAt());

        return DebateRoomResponse.builder()
                .roomId(room.getRoomId())
                .title(room.getTitle())
                .status(room.getStatus().name())
                .category(DebateRoomResponse.CategoryDto.builder()
                        .id(room.getCategoryId())
                        .name("카테고리 이름은 추후 조회") // 카테고리 명 로직 추가 필요
                        .build())
                .sideA(room.getSideA())
                .sideB(room.getSideB())
                .maxSpeaker(room.getMaxSpeaker())
                .maxAudience(room.getMaxAudience())
                .currentSpeaker(currentSpeaker)
                .currentAudience(currentAudience)
                .elapsedSeconds(elapsedSeconds)
                .build();
    }

    // 토론방 만들기
    public DebateRoom createRoom(CreateRoomRequest request) {
        DebateRoom debateRoom = new DebateRoom();

        debateRoom.setRoomId(request.getRoomId()); // 수동 설정 필요 없다면 생략
        debateRoom.setUserId(request.getUserId());
        debateRoom.setTitle(request.getTitle()); //토론 주제
        debateRoom.setDebateDescription(request.getDebateDescription()); //토론 설명

        // 카테고리 ID 추출
        if (request.getCategory() != null) {
            debateRoom.setCategoryId(request.getCategory().getId());
        }

        debateRoom.setSideA(request.getSideA()); // 토론 사이드 (찬성)
        debateRoom.setSideB(request.getSideB()); // 토론 사이드 (반대)

        debateRoom.setDebateType(request.getDebateType()); // 토론 유형
        debateRoom.setDurationSeconds(request.getDurationSeconds()); //토론 시간
        debateRoom.setMaxSpeaker((long) request.getMaxSpeaker()); // 최대 발언자 수
        debateRoom.setMaxAudience((long) request.getMaxAudience()); //최대 청중 수

        debateRoom.setStatus(DebateRoomStatus.waiting); // enum 값 명확히 지정
        debateRoom.setCreatedAt(LocalDateTime.now());

        // maxParticipants는 요청에 없으면 계산해서 넣어야 할 수도 있음
        debateRoom.setMaxParticipants(
            (long) (request.getMaxSpeaker() + request.getMaxAudience())
        );

        return debateRoomRepository.save(debateRoom);
    }

}
