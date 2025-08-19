package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.dto.DebateRoomResponse;
import com.likelion.realtalk.domain.debate.entity.DebateRoom;
import com.likelion.realtalk.domain.debate.repository.DebateRoomRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DebateRoomMatchService {

  private final DebateRoomRepository debateRoomRepository;
  private final RoomIdMappingService roomIdMappingService;
  private final RedisRoomTracker redisRoomTracker;

  public DebateRoomResponse matchOne(Long categoryId) {
    // 1) 후보 방: 해당 카테고리의 waiting/started 상태의 방 후보 조회
    List<DebateRoom> rooms = debateRoomRepository.findByCategoryAndStatus(categoryId);
    log.info("조회한 방 리스트 크기: {}", rooms.size());
    if (rooms == null || rooms.isEmpty()) {
      throw new NoSuchElementException("해당 카테고리의 토론방이 없습니다. categoryId=" + categoryId);
    }

    // 2) Redis 현재 인원으로 "입장 가능" 필터 중 청중 수 가장 많은 방 선택
    DebateRoom picked = null;
    long pickedCurSpeaker = 0L;
    long pickedCurAudience = 0L;

    for (DebateRoom r : rooms) {
      long curAudience = redisRoomTracker.getCurrentAudiences(r.getRoomId());
      long maxAudience = nz(r.getMaxAudience());
      log.info("방 ID: {}, 현재 청중 수: {}, 최대 청중 수: {}", r.getRoomId(), curAudience, maxAudience);

      // 청중석 만석이면 다음 방으로
      if (curAudience >= maxAudience) {
        log.info("방 ID: {} 청중석 만석으로 건너뜀", r.getRoomId());
        continue;
      }

      long curSpeaker = Long.MIN_VALUE;

      // 타이브레이커에서 speaker 수가 필요할 수 있으므로, 동률일 때만 lazy 조회 보정
      if (picked != null && curAudience == pickedCurAudience) {
        curSpeaker = redisRoomTracker.getCurrentSpeakers(r.getRoomId());
        if (pickedCurSpeaker == Long.MIN_VALUE) {
          pickedCurSpeaker = redisRoomTracker.getCurrentSpeakers(picked.getRoomId());
        }
      }

      if (picked == null
          || curAudience > pickedCurAudience
          || (curAudience == pickedCurAudience && curSpeaker < pickedCurSpeaker)
          || (curAudience == pickedCurAudience && curSpeaker == pickedCurSpeaker && r.getRoomId() < picked.getRoomId())) {
        String reason = picked == null ? "첫 후보 방" :
            curAudience > pickedCurAudience ? "청중 수 많음" :
            (curAudience == pickedCurAudience && curSpeaker < pickedCurSpeaker) ? "스피커 수 적음" :
            "roomId 작은 방";
        log.info("후보 방 갱신: 이전 방 ID: {}, 새 방 ID: {}, 이유: {}", picked == null ? "없음" : picked.getRoomId(), r.getRoomId(), reason);
        picked = r;
        pickedCurSpeaker = curSpeaker;
        pickedCurAudience = curAudience;
      }
    }

    if (picked == null) {
      throw new NoSuchElementException(
          "해당 카테고리의 대기/시작 중인 방 중 입장 가능한 방이 없습니다. categoryId=" + categoryId);
    }
    // 3) PK -> UUID 매핑
    UUID roomUuid = roomIdMappingService.toUuid(picked.getRoomId());
    log.info("최종 선택된 방 ID: {}, UUID: {}, currentSpeaker: {}, currentAudience: {}", picked.getRoomId(), roomUuid, pickedCurSpeaker, pickedCurAudience);

    // 4) Redis에서 최종 카운트 재조회 대신 캐시 값 사용
    long currentSpeaker  = pickedCurSpeaker;
    long currentAudience = pickedCurAudience;

    // 5) 경과 시간(초) 계산 (waiting 상태는 0, started만 계산)
    String st = picked.getStatus() == null ? null : picked.getStatus().name();
    long elapsedSeconds = ("waiting".equalsIgnoreCase(st))
        ? 0L
        : calculateElapsedSeconds(picked.getStartedAt());
    log.info("경과 시간(초) 계산 결과: {}", elapsedSeconds);

    // 6) findRoomById와 동일 형식으로 응답 빌드
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
