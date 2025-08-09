package com.likelion.realtalk.debate.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.likelion.realtalk.debate.dto.AiSummaryResponse;
import com.likelion.realtalk.debate.dto.CreateRoomRequest;
import com.likelion.realtalk.debate.dto.DebateRoomResponse;
import com.likelion.realtalk.debate.entity.DebateRoom;
import com.likelion.realtalk.debate.entity.DebateRoomStatus;
import com.likelion.realtalk.debate.repository.DebateRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DebateRoomService {

    private final DebateRoomRepository debateRoomRepository;
    private final RedisRoomTracker redisRoomTracker;
    private final RoomIdMappingService roomIdMappingService;
    // private final StringRedisTemplate stringRedisTemplate; // ← 주입 추가
    // private final DebateEventPublisher debateEventPublisher;

    private Long calculateElapsedSeconds(LocalDateTime startedAt) {
        if (startedAt == null) return 0L;
        return Duration.between(startedAt, LocalDateTime.now()).getSeconds();
    }

    public DebateRoom findRoomSummaryById(Long roomId) {
        return debateRoomRepository.findById(roomId).orElse(null);
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
        if (rooms.isEmpty()) return List.of();

        // 1) PK 목록 준비
        List<Long> pks = rooms.stream().map(DebateRoom::getRoomId).toList();

        // 2) PK → UUID 일괄 조회
        Map<Long, UUID> pkToUuid = roomIdMappingService.toUuidBatch(pks);

        // 3) 매핑 누락 정책: 여기서는 누락 시 skip (원하시면 throw로 바꾸세요)
        return rooms.stream()
            .map(room -> {
                UUID externalId = pkToUuid.get(room.getRoomId());
                if (externalId == null) {
                    // throw new IllegalStateException("PK 매핑 없음: " + room.getRoomId());
                    return null; // skip 정책
                }

                Long currentSpeaker  = redisRoomTracker.getCurrentSpeakers(room.getRoomId());
                Long currentAudience = redisRoomTracker.getCurrentAudiences(room.getRoomId());
                Long elapsedSeconds  = calculateElapsedSeconds(room.getCreatedAt());

                return DebateRoomResponse.builder()
                        .roomId(externalId)
                        .title(room.getTitle())
                        .status(room.getStatus().name())
                        .category(DebateRoomResponse.CategoryDto.builder()
                                .id(room.getCategoryId())
                                .name("카테고리 이름은 추후 조회")
                                .build())
                        .sideA(room.getSideA())
                        .sideB(room.getSideB())
                        .maxSpeaker(room.getMaxSpeaker())
                        .maxAudience(room.getMaxAudience())
                        .currentSpeaker(currentSpeaker)
                        .currentAudience(currentAudience)
                        .elapsedSeconds(elapsedSeconds)
                        .build();
            })
            .filter(Objects::nonNull) // skip 정책일 때만
            .toList();
    }

    // roomId로 토론방 검색
    public DebateRoomResponse findRoomById(UUID roomUuid) {
        Long pk = roomIdMappingService.toPk(roomUuid);

        DebateRoom room = debateRoomRepository.findById(pk)
            .orElseThrow(() -> new RuntimeException("토론방을 찾을 수 없습니다."));

        Long currentSpeaker  = redisRoomTracker.getCurrentSpeakers(pk);
        Long currentAudience = redisRoomTracker.getCurrentAudiences(pk);
        Long elapsedSeconds  = calculateElapsedSeconds(room.getCreatedAt());

        return DebateRoomResponse.builder()
                .roomId(roomUuid) // ★ 여기! Long이 아니라 UUID를 넣어야 함
                .title(room.getTitle())
                .status(room.getStatus().name())
                .category(DebateRoomResponse.CategoryDto.builder()
                        .id(room.getCategoryId())
                        .name("카테고리 이름은 추후 조회")
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

        DebateRoom saved = debateRoomRepository.save(debateRoom);

        UUID uuid = UUID.randomUUID();

        // TODO: getID()
        roomIdMappingService.put(uuid, saved.getRoomId());

        return debateRoomRepository.save(debateRoom);
    }

}
