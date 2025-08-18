package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.dto.DebateRoomResponse;
import com.likelion.realtalk.domain.debate.entity.DebateRoom;
import com.likelion.realtalk.domain.debate.repository.DebateRoomRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DebateRoomMatchService {

  private final DebateRoomRepository debateRoomRepository;
  private final RoomIdMappingService roomIdMappingService;
  private final RedisRoomTracker redisRoomTracker;

  public DebateRoomResponse matchOne(Long categoryId) {
    // 1) 후보 방: 해당 카테고리 + ACTIVE
    List<DebateRoom> rooms = debateRoomRepository.pickOneByCategory(categoryId);
    if (rooms == null || rooms.isEmpty()) {
      throw new NoSuchElementException("해당 카테고리의 ACTIVE 방이 없습니다. categoryId=" + categoryId);
    }

    // 2) Redis 현재 인원으로 "입장 가능" 필터
    List<DebateRoom> candidates = new ArrayList<>();
    for (DebateRoom r : rooms) {
      long curSpeaker = redisRoomTracker.getCurrentSpeakers(r.getRoomId());
      long curAudience = redisRoomTracker.getCurrentAudiences(r.getRoomId());
      long maxSpeaker = nz(r.getMaxSpeaker());
      long maxAudience = nz(r.getMaxAudience());

      if (curSpeaker < maxSpeaker || curAudience < maxAudience) {
        candidates.add(r);
      }
    }
    if (candidates.isEmpty()) {
      throw new NoSuchElementException("해당 카테고리의 ACTIVE 방 중 입장 가능한 방이 없습니다. categoryId=" + categoryId);
    }

    // 3) 후보 중 랜덤 선택
    DebateRoom picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

    // 4) PK -> UUID 매핑
    UUID roomUuid = roomIdMappingService.toUuid(picked.getRoomId());

    // 5) Redis에서 최종 카운트 재조회 (선택 사항: 위에서 구한 값 재사용해도 무방)
    long currentSpeaker  = redisRoomTracker.getCurrentSpeakers(picked.getRoomId());
    long currentAudience = redisRoomTracker.getCurrentAudiences(picked.getRoomId());

    // 6) 경과 시간(초) 계산 (방 전체 진행 시간)
    long elapsedSeconds = calculateElapsedSeconds(picked.getStartedAt());

    // 7) findRoomById와 동일 형식으로 응답 빌드
    return DebateRoomResponse.builder()
        .roomId(roomUuid) // ★ UUID
        .title(picked.getTitle())
        .status(picked.getStatus().name())
        .category(DebateRoomResponse.CategoryDto.builder()
            .id(picked.getCategory().getId())
            .name(picked.getCategory().getCategoryName())
            .build())
        .sideA(picked.getSideA())
        .sideB(picked.getSideB())
        .maxSpeaker(picked.getMaxSpeaker())
        .maxAudience(picked.getMaxAudience())
        .debateType(picked.getDebateType())
        .currentSpeaker(currentSpeaker)
        .currentAudience(currentAudience)
        .createUserId(picked.getUserId())
        .elapsedSeconds(elapsedSeconds)
        .build();
  }

  private long nz(Long v) {
    return v == null ? 0L : v;
  }

  private Long calculateElapsedSeconds(LocalDateTime startedAt) {
    if (startedAt == null) {
      return 0L;
    }
    return Duration.between(startedAt, LocalDateTime.now(ZoneId.of("Asia/Seoul"))).getSeconds();
  }

}
