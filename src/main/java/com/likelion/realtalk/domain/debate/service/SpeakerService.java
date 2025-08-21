package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.dto.AiSummaryDto;
import com.likelion.realtalk.domain.debate.dto.DebateMessageDto;
import com.likelion.realtalk.domain.debate.dto.DebateRoomDto;
import com.likelion.realtalk.domain.debate.dto.RoomUserInfo;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerTimerDto;
import com.likelion.realtalk.domain.debate.entity.DebateRoom;
import com.likelion.realtalk.domain.debate.entity.DebateRoomStatus;
import com.likelion.realtalk.domain.debate.repository.DebateRedisRepository;
import com.likelion.realtalk.domain.debate.repository.DebateRoomRepository;
import com.likelion.realtalk.global.exception.DebateRoomValidationException;
import com.likelion.realtalk.global.exception.ErrorCode;
import com.likelion.realtalk.global.redis.RedisKeyUtil;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SpeakerService {

  private final DebateRedisRepository debateRedisRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final AiService aiService;
  private final RoomIdMappingService roomIdMappingService;
  private final DebateRoomRepository debateRoomRepository;

  // 토론방 최초 입장 시 발언 내용들을 조회하는 메서드
  public ArrayList<SpeakerMessageDto> getSpeeches(String roomUUID) {
    String turnValue = debateRedisRepository.getRoomField(roomUUID, "turn");
    if (turnValue == null) {
      return new ArrayList<>(); // 또는 예외 던짐
    }
    int turn = Integer.parseInt(turnValue);

    ArrayList<SpeakerMessageDto> speakerMessageDtos = new ArrayList<>();
    String speecheKey = RedisKeyUtil.getSpeechesKey(roomUUID);
    for (int i = 1; i <= turn; i++) {

      List<SpeakerMessageDto> speeches = debateRedisRepository.getSpeeches(speecheKey,
          String.valueOf(i));

      if (speeches != null && !speeches.isEmpty()) {
        speakerMessageDtos.addAll(speeches);
      }
    }

    return speakerMessageDtos;
  }

  public SpeakerTimerDto getSpeakerExpire(String roomUUID) {
    return SpeakerTimerDto.builder()
        .speakerExpireTime(debateRedisRepository.getRedisValue(RedisKeyUtil.getExpireKey(roomUUID)))
        .currentUserId(debateRedisRepository.getRoomField(roomUUID, "currentSpeaker")).build();
  }

  // 사용자 모두 입장시 최초 실행 메서드 > 이후 입장 정책이 변경되면 로직 변경 가능성 있음
  public void setDebateRoom(DebateRoomDto dto) {
    String roomUUID = String.valueOf(dto.getRoomUUID());

    // 1. 토론방 타입 지정
    debateRedisRepository.saveRoomField(roomUUID, "debateType", dto.getDebateType().toString());

    // 2. 토론방 참여자 지정
    List<String> participants = new ArrayList<>();
    for (Long userId : dto.getUserIds()) {
      participants.add(String.valueOf(userId));
    }
    debateRedisRepository.saveParticipants(roomUUID, participants);

    // 3. 첫번째 턴 시작
    startTurn(roomUUID, "1");
  }

  // turnNo를 받아 새로운 turn 시작 메서드
  private void startTurn(String roomUUID, String turnNo) {
    List<String> participants = debateRedisRepository.getParticipants(roomUUID);
    String firstUserId = null;

    if (!participants.isEmpty()) {
      // 1. turn의 첫 시작이기 때문에 첫번째 발언자 시작
      firstUserId = participants.get(0);
    }

    // 2. turnNo, currentSpeaker 지정
    debateRedisRepository.saveRoomField(roomUUID, "turn", turnNo);
    debateRedisRepository.saveRoomField(roomUUID, "currentSpeaker", firstUserId);

    // 3. spokenUsers 초기화
    debateRedisRepository.saveSpokenUsers(roomUUID, new ArrayList<>());

    // 4. 발언 타이머 설정
    pubSpeakerExpireTimer(roomUUID);
  }

  // 발언 타이머 내 발언 메서드
  public void submitSpeech(DebateMessageDto dto) {
    String roomUUID = dto.getRoomUUID();
    String turnNo = debateRedisRepository.getRoomField(roomUUID, "turn");

    // 1. ai 팩트체킹
    SpeakerMessageDto speakerMessageDto = aiService.factcheck(dto.getMessage());

    // 2. 기존 발언들 조회
    List<SpeakerMessageDto> speeches = debateRedisRepository.getSpeeches(
        RedisKeyUtil.getSpeechesKey(roomUUID), turnNo);
    speakerMessageDto.setMessage(dto.getMessage());
    speakerMessageDto.setUserId(dto.getUserId());
    speakerMessageDto.setSide(dto.getSide());
    RoomUserInfo info = debateRedisRepository.getSpeaker(
        roomIdMappingService.toPk(UUID.fromString(roomUUID)), dto.getUserId());
    speakerMessageDto.setUsername(info != null ? info.getUserName() : "UNKNOWN");

    speeches.add(speakerMessageDto);

    // 3. 발언 내용 추가
    debateRedisRepository.saveSpeeches(RedisKeyUtil.getSpeechesKey(roomUUID), turnNo, speeches);

    // 4. 발언 내용 pub
    messagingTemplate.convertAndSend("/topic/speaker/" + roomUUID, speakerMessageDto);

    // 5. 발언 타이머 expire 처리 -> 발언 막지 않음
    // debateRedisRepository.expireTime(RedisKeyUtil.getExpireKey(roomUUID));

    // 6. AI 요약 redis 저장

    // 6-1. AI 요약 LIST 조회
    String aiSummaryKey = RedisKeyUtil.getAiSummariesKey(roomUUID);
    List<AiSummaryDto> aiSummaryDtos = debateRedisRepository.getAiSummaries(aiSummaryKey, turnNo);

    // 6-2. 요약 결과 LIST에 추가 및 저장
    AiSummaryDto aiSummaryDto = aiService.summary(dto.getMessage());
    aiSummaryDto.setUserId(dto.getUserId());
    aiSummaryDto.setSide(dto.getSide());
    RoomUserInfo userInfo = debateRedisRepository.getSpeaker(
        roomIdMappingService.toPk(UUID.fromString(roomUUID)), dto.getUserId());
    aiSummaryDto.setUsername(userInfo != null ? userInfo.getUserName() : "UNKNOWN");
    aiSummaryDtos.add(aiSummaryDto);
    debateRedisRepository.saveAiSummaries(aiSummaryKey, turnNo, aiSummaryDtos);

    // 7. 발언 AI 요약 pub
    messagingTemplate.convertAndSend("/topic/ai/" + roomUUID, aiSummaryDto);
  }

  // 다음 발언자가 있으면 발언자 지정, 발언자가 없으면 다음 턴으로 넘어감
  public void startNextSpeaker(String roomUUID) {
    // 1. 발언 완료자에 추가
    List<String> spokenUsers = debateRedisRepository.getSpokenUsers(roomUUID);
    String userIdStr = debateRedisRepository.getRoomField(roomUUID, "currentSpeaker");

    if (!spokenUsers.contains(userIdStr)) {
      spokenUsers.add(userIdStr);
      debateRedisRepository.saveSpokenUsers(roomUUID, spokenUsers);
    }
    String nextUserId = getNextUserId(roomUUID);
    // 2. 다음 발언자 지정
    if (nextUserId != null) {
      // 2-1. 다음 발언자 지정
      debateRedisRepository.saveRoomField(roomUUID, "currentSpeaker", nextUserId);

      // 2-2. 발언 타이머 시작
      pubSpeakerExpireTimer(roomUUID);
      // 3. 다음 턴 시작
    } else {
      // 3-1. 다음 턴으로 변경
      debateRedisRepository.saveRoomField(roomUUID, "turn", String.valueOf(
          (Integer.parseInt(debateRedisRepository.getRoomField(roomUUID, "turn")) + 1)));

      // 3-2. 다음 턴 시작
      startTurn(roomUUID, debateRedisRepository.getRoomField(roomUUID, "turn"));
    }
  }

  // 청중 토론 종료 후 해당 메서드를 통해 nextUserId가 있는지 없는지 확인 후 startTurn을 진행할지, 다음 사람을 지정할지 결정
  private String getNextUserId(String roomUUID) {
    List<String> participants = debateRedisRepository.getParticipants(roomUUID);
    List<String> spokenUsers = debateRedisRepository.getSpokenUsers(roomUUID);

    if (spokenUsers.size() >= participants.size()) {
      return null;
    }
    return participants.stream().filter(p -> !spokenUsers.contains(p)).findFirst().orElseGet(() -> {
      return null;
    });
  }

  // 발언 시간 타이머 발행
  private void pubSpeakerExpireTimer(String roomUUID) {
    String expireTime = debateRedisRepository.setExpireTime(roomUUID,
        RedisKeyUtil.getExpireKey(roomUUID));
    SpeakerTimerDto speakerTimerDto = SpeakerTimerDto.builder().speakerExpireTime(expireTime)
        .currentUserId(debateRedisRepository.getRoomField(roomUUID, "currentSpeaker")).build();
    messagingTemplate.convertAndSend("/topic/speaker/" + roomUUID + "/expire", speakerTimerDto);
  }

  // redis 정보 삭제
  public void clearSpeakerCaches(String roomUUID) {
    this.debateRedisRepository.deleteByKey(RedisKeyUtil.getSpeechesKey(roomUUID));
    this.debateRedisRepository.deleteByKey(RedisKeyUtil.getRoomKey(roomUUID));
    this.debateRedisRepository.deleteByKey(RedisKeyUtil.getExpireKey(roomUUID));
  }

  public void validateSpeaker(DebateMessageDto dto) {
    String expireTime = this.debateRedisRepository.getRedisValue(
        RedisKeyUtil.getExpireKey(dto.getRoomUUID()));
    String currentUserId = this.debateRedisRepository.getRoomField(dto.getRoomUUID(),
        "currentSpeaker");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime targetTime = LocalDateTime.parse(expireTime, formatter);

    // 현재 한국 시간
    LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    if (now.isAfter(targetTime)) {
      throw new IllegalStateException("이미 시간이 지났습니다. (마감: " + expireTime + ")");
    }

    if (!currentUserId.equals(String.valueOf(dto.getUserId()))) {
      throw new IllegalStateException("현재 발언자가 아닙니다.");
    }
  }

  // 발언자가 websocket 연결 해제됐을 때
  public void disconnectParticipant(String roomUUID, Long userId) {
    String disconnectedUserId = String.valueOf(userId);
    // 현재 발언중인 발언자가 토론방을 나갔을 때
    if (debateRedisRepository.getRedisValue(RedisKeyUtil.getExpireKey(roomUUID)) != null
        && debateRedisRepository.getRoomField(roomUUID, "currentSpeaker")
        .equals(disconnectedUserId)) {
      // 청중 타임으로 넘겨버림
      debateRedisRepository.expireTime(RedisKeyUtil.getExpireKey(roomUUID));
    }

    List<String> participants = debateRedisRepository.getParticipants(roomUUID);
    List<String> spokenUsers = debateRedisRepository.getSpokenUsers(roomUUID);

    if (participants != null) {
      participants.removeIf(participant -> participant.equals(disconnectedUserId));
      debateRedisRepository.saveParticipants(roomUUID, participants); // Redis에 저장
    }

    if (spokenUsers != null) {
      spokenUsers.removeIf(spokenUser -> spokenUser.equals(disconnectedUserId));
      debateRedisRepository.saveSpokenUsers(roomUUID, spokenUsers); // Redis에 저장
    }
  }

  public void addParticipant(String roomUUID,Long roomId, Long userId) {
    DebateRoom room = debateRoomRepository.findById(roomId).orElseThrow(() -> new DebateRoomValidationException(
        ErrorCode.DEBATE_NOT_FOUND));

    // 이미 시작중일 경우에만 넣는다.
    if(room.getStatus().equals(DebateRoomStatus.started)) {
      String newUserId = String.valueOf(userId);
      List<String> participants = debateRedisRepository.getParticipants(roomUUID);
      if (participants != null) {
        participants.removeIf(participant -> participant.equals(newUserId));
      }
      participants.add(newUserId);
      debateRedisRepository.saveParticipants(roomUUID, participants); // Redis에 저장
    }
  }

}
