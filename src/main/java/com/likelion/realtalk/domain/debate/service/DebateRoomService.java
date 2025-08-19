package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.category.entity.Category;
import com.likelion.realtalk.domain.category.repository.CategoryRepository;
import com.likelion.realtalk.domain.debate.auth.AuthUserPrincipal;
import com.likelion.realtalk.domain.debate.dto.DebateRoomDto;
import com.likelion.realtalk.domain.debate.dto.DebateRoomTimerDto;
import com.likelion.realtalk.domain.debate.dto.DebatestartResponse;
import com.likelion.realtalk.domain.debate.repository.DebateRedisRepository;
import com.likelion.realtalk.global.exception.CustomException;
import com.likelion.realtalk.global.exception.DebateRoomValidationException;
import com.likelion.realtalk.global.exception.ErrorCode;
import com.likelion.realtalk.global.redis.RedisKeyUtil;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.likelion.realtalk.domain.debate.dto.AiSummaryResponse;
import com.likelion.realtalk.domain.debate.dto.CreateRoomRequest;
import com.likelion.realtalk.domain.debate.dto.DebateRoomResponse;
import com.likelion.realtalk.domain.debate.entity.DebateRoom;
import com.likelion.realtalk.domain.debate.entity.DebateRoomStatus;
import com.likelion.realtalk.domain.debate.repository.DebateRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebateRoomService {

  private final DebateRoomRepository debateRoomRepository;
  private final RedisRoomTracker redisRoomTracker;
  private final RoomIdMappingService roomIdMappingService;
  private final CategoryRepository categoryRepository;
  private final DebateRedisRepository debateRedisRepository;
  private final SimpMessageSendingOperations messagingTemplate;
  private final SpeakerService speakerService;
  // private final StringRedisTemplate stringRedisTemplate; // ← 주입 추가
  // private final DebateEventPublisher debateEventPublisher;

  private Long calculateElapsedSeconds(LocalDateTime startedAt) {
    if (startedAt == null) {
      return 0L;
    }
    return Duration.between(startedAt, LocalDateTime.now(ZoneId.of("Asia/Seoul"))).getSeconds();
  }

  public DebateRoom findRoomSummaryById(Long roomId) {
    return debateRoomRepository.findById(roomId).orElse(null);
  }

  //AI 정리
  public AiSummaryResponse findAiSummaryById(Long roomId) {
    DebateRoom room = debateRoomRepository.findById(roomId)
        .orElseThrow(() -> new DebateRoomValidationException(ErrorCode.DEBATE_NOT_FOUND));

    return AiSummaryResponse.builder()
        .roomId(room.getRoomId())
        .title(room.getTitle())
        .category(AiSummaryResponse.CategoryDto.builder()
            .id(room.getCategory().getId())
            .name("카테고리 이름은 추후 조회") // 카테고리 명 로직 추가 필요
            .build())
        .build();
  }

  //모든 토론방 조회
  public List<DebateRoomResponse> findAllRooms() {
    List<DebateRoom> rooms = debateRoomRepository.findAllWithCategory();
    if (rooms.isEmpty()) {
      return List.of();
    }

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

          Long currentSpeaker = redisRoomTracker.getCurrentSpeakers(room.getRoomId());
          Long currentAudience = redisRoomTracker.getCurrentAudiences(room.getRoomId());
          Long elapsedSeconds =
              room.getStatus().equals(DebateRoomStatus.ended) ? room.getDurationSeconds()
                  : calculateElapsedSeconds(room.getStartedAt());

          return DebateRoomResponse.builder()
              .roomId(externalId)
              .title(room.getTitle())
              .status(room.getStatus().name())
              .category(DebateRoomResponse.CategoryDto.builder()
                  .id(room.getCategory().getId())
                  .name(room.getCategory().getCategoryName())
                  .build())
              .sideA(room.getSideA())
              .sideB(room.getSideB())
              .maxSpeaker(room.getMaxSpeaker())
              .maxAudience(room.getMaxAudience())
              .createUserId(room.getUserId())
              .debateType(room.getDebateType())
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
        .orElseThrow(() -> new DebateRoomValidationException(ErrorCode.DEBATE_NOT_FOUND));

    Long currentSpeaker = redisRoomTracker.getCurrentSpeakers(pk);
    Long currentAudience = redisRoomTracker.getCurrentAudiences(pk);
    Long elapsedSeconds = calculateElapsedSeconds(room.getStartedAt());

    return DebateRoomResponse.builder()
        .roomId(roomUuid) // ★ 여기! Long이 아니라 UUID를 넣어야 함
        .title(room.getTitle())
        .status(room.getStatus().name())
        .category(DebateRoomResponse.CategoryDto.builder()
            .id(room.getCategory().getId())
            .name(room.getCategory().getCategoryName())
            .build())
        .sideA(room.getSideA())
        .sideB(room.getSideB())
        .maxSpeaker(room.getMaxSpeaker())
        .maxAudience(room.getMaxAudience())
        .debateType(room.getDebateType())
        .currentSpeaker(currentSpeaker)
        .currentAudience(currentAudience)
        .createUserId(room.getUserId())
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

    Long categoryId = request.getCategory().getId();
    Category category = categoryRepository.findById(categoryId)
        .orElseThrow(() -> new IllegalArgumentException("해당 카테고리 정보를 찾을 수 없습니다."));

    // 카테고리 ID 추출
    if (request.getCategory() != null) {
      debateRoom.setCategory(category);
    }

    debateRoom.setSideA(request.getSideA()); // 토론 사이드 (찬성)
    debateRoom.setSideB(request.getSideB()); // 토론 사이드 (반대)

    debateRoom.setDebateType(request.getDebateType()); // 토론 유형
    debateRoom.setDurationSeconds(request.getDurationSeconds()); //토론 시간
    debateRoom.setMaxSpeaker((long) request.getMaxSpeaker()); // 최대 발언자 수
    debateRoom.setMaxAudience((long) request.getMaxAudience()); //최대 청중 수

    debateRoom.setStatus(DebateRoomStatus.waiting); // enum 값 명확히 지정
    debateRoom.setStartedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));

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

  @Transactional
  public DebatestartResponse startRoom(Long roomId, UUID roomUUID) {
    int rows = debateRoomRepository.startIfWaiting(
        roomId,
        LocalDateTime.now(ZoneId.of("Asia/Seoul")),
        DebateRoomStatus.started,
        DebateRoomStatus.waiting
    );

    if (rows == 0) {
      if (!debateRoomRepository.existsById(roomId)) {
        throw new CustomException(ErrorCode.NOT_FOUND) {
        };
      }
      throw new CustomException(ErrorCode.ROOM_ALREADY_STARTED) {
      };
    }

    // 갱신된 엔티티 재조회 후 DTO 구성
    DebateRoom room = debateRoomRepository.findById(roomId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND) {
        });

    DebateRoomResponse.CategoryDto cat = null;
    if (room.getCategory() != null) {
      cat = DebateRoomResponse.CategoryDto.builder()
          .id(room.getCategory().getId())
          .name(room.getCategory().getCategoryName())
          .build();
    }

    // 전체 토론 시작
    startDebateTime(roomUUID, room);

    speakerService.setDebateRoom(DebateRoomDto.builder()
        .roomUUID(roomUUID.toString())
        .userIds(debateRedisRepository.getSpeakerUserIds(roomId))
        .debateType(room.getDebateType())
        .build());

    return DebatestartResponse.builder()
        .status(room.getStatus() != null ? room.getStatus().name() : null)
        .startedAt(room.getStartedAt())                 // 시작 시각 포함
        .build();
  }

  public void startDebateTime(UUID roomUUID, DebateRoom debateRoom) {
    // 전체 토론 타이머 설정
    LocalDateTime expireTime = debateRoom.getStartedAt()
        .plusSeconds(debateRoom.getDurationSeconds());
    Duration duration = Duration.between(LocalDateTime.now(ZoneId.of("Asia/Seoul")), expireTime);

    debateRedisRepository.putValueWithExpire(RedisKeyUtil.getDebateRoomExpire(roomUUID.toString()),
        expireTime.toString(), duration);

    debateRedisRepository.saveRoomField(roomUUID.toString(), "debateRoomExpire",
        expireTime.toString());

    DebateRoomTimerDto dto = DebateRoomTimerDto.builder().debateExpireTime(expireTime.toString())
        .build();
    messagingTemplate.convertAndSend("/topic/debate/" + roomUUID + "/expire", dto);
  }

  public void extendDebateTime(String roomUUID, Principal principal) {
    String expiredTime = debateRedisRepository.getRedisValue(
        RedisKeyUtil.getDebateRoomExpire(roomUUID));
    if (expiredTime == null) {
      throw new DebateRoomValidationException(ErrorCode.INVALID_DEBATE_STATE);
    }

    LocalDateTime parsed = LocalDateTime.parse(expiredTime); // 기본 ISO 파서

    Duration plusDuration = debateRedisRepository.getDebateTime(roomUUID);

    Duration diff = Duration.between(LocalDateTime.now(ZoneId.of("Asia/Seoul")), parsed);
    if(diff.compareTo(plusDuration) > 0) {
      // 연장 가능한 시간이 아닐 경우 예외처리
      throw new DebateRoomValidationException(ErrorCode.INVALID_EXTENSION_REQUEST);
    }

    Authentication authentication = (Authentication) principal;

    if (authentication.isAuthenticated()) {
      // 로그인이 되어 있을 경우
      AuthUserPrincipal user = (AuthUserPrincipal) authentication.getPrincipal();

      Long roomId = roomIdMappingService.toPk(UUID.fromString(roomUUID));
      List<String> extensionRequesters = this.debateRedisRepository.getExtensionRequesters(roomUUID);

      // 발언자 userIds 조회
      List<Long> speakerUserIds = debateRedisRepository.getSpeakerUserIds(roomId);
      if (!speakerUserIds.contains(user.getUserId())) {
        // 발언자가 아닐 경우 예외 처리
        throw new DebateRoomValidationException(ErrorCode.ACCESS_DENIED);
      }

      String userId = String.valueOf(user.getUserId());
      extensionRequesters.removeIf(requester -> requester.equals(userId));
      extensionRequesters.add(userId);

      if (speakerUserIds.size() / 2 < extensionRequesters.size()) {
        // 과반수 이상 연장 요청 시 연장
        log.info("과반수 이상 토론 연장");

        LocalDateTime newExpireTime = parsed.plus(plusDuration);
        Duration duration = Duration.between(LocalDateTime.now(ZoneId.of("Asia/Seoul")),
            newExpireTime);

        debateRedisRepository.putValueWithExpire(RedisKeyUtil.getDebateRoomExpire(roomUUID),
            newExpireTime.toString(), duration);

        debateRedisRepository.saveRoomField(roomUUID, "debateRoomExpire", newExpireTime.toString());

        DebateRoomTimerDto dto = DebateRoomTimerDto.builder()
            .debateExpireTime(newExpireTime.toString())
            .build();

        messagingTemplate.convertAndSend("/topic/debate/" + roomUUID + "/expire", dto);
        this.debateRedisRepository.saveExtensionRequesters(roomUUID, new ArrayList<>());
      } else {
        log.info("과반수 미만 토론 연장자 저장");
        this.debateRedisRepository.saveExtensionRequesters(roomUUID, extensionRequesters);
      }
    } else {
      // 로그인 안 되어 있을 경우 예외처리
      throw new DebateRoomValidationException(ErrorCode.UNAUTHORIZED);
    }
  }

  @Transactional
  public void endDebate(String roomUUID) {
    Long roomId = roomIdMappingService.toPk(UUID.fromString(roomUUID));

    DebateRoom room = debateRoomRepository.findById(roomId)
        .orElseThrow(() -> new DebateRoomValidationException(ErrorCode.DEBATE_NOT_FOUND));

    String expiredTime = debateRedisRepository.getRoomField(roomUUID, "debateRoomExpire");
    LocalDateTime closedAt = LocalDateTime.parse(expiredTime);

    room.endDebate(closedAt);
  }

  public void pubEndDebate(String roomUUID) {
    messagingTemplate.convertAndSend("/topic/debate/" + roomUUID + "/end", "ENDED");
  }

  public DebateRoomTimerDto getDebateRoomExpireTime(String roomUUID) {
    return DebateRoomTimerDto.builder().debateExpireTime(
        debateRedisRepository.getRedisValue(RedisKeyUtil.getDebateRoomExpire(roomUUID))).build();
  }
}
