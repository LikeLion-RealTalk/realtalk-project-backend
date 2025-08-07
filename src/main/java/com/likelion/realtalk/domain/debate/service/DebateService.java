package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.dto.DebateMessageDto;
import com.likelion.realtalk.domain.debate.dto.DebateRoomDto;
import com.likelion.realtalk.domain.debate.repository.DebateRedisRepository;
import com.likelion.realtalk.global.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DebateService {

  private final DebateRedisRepository debateRedisRepository;

  // 사용자 모두 입장시 최초 실행 메서드 > 이후 입장 정책이 변경되면 로직 변경 가능성 있음
  public void setDebateRoom(DebateRoomDto dto) {
    String roomId = String.valueOf(dto.getRoomId());

    // 1. 토론방 타입 지정
    debateRedisRepository.saveRoomField(roomId, "debateType", dto.getDebateType());

    // 2. 토론방 참여자 지정
    Map<String, String> participantMap = new LinkedHashMap<>();
    int index = 0;
    for (Long userId : dto.getUserIds()) {
      participantMap.put(String.valueOf(index++), userId.toString());
    }
    debateRedisRepository.saveParticipants(roomId, participantMap);

    // 3. 첫번째 턴 시작
    startTurn(roomId, "1");
  }

  // turnNo를 받아 새로운 turn 시작 메서드
  public void startTurn(String roomId, String turnNo) {
    List<String> participants = debateRedisRepository.getParticipants(roomId);
    if (participants.isEmpty()) {
      return;
    }

    // 1. turn의 첫 시작이기 때문에 첫번째 발언자 시작
    String firstUserId = participants.get(0);

    // 2. turnNo, currentSpeaker 지정
    debateRedisRepository.saveRoomField(roomId, "turn", turnNo);
    debateRedisRepository.saveRoomField(roomId, "currentSpeaker", firstUserId);

    // 3. spokenUsers 초기화
    debateRedisRepository.saveSpokenUsers(roomId, new ArrayList<>());

    // 4. 발언 타이머 설정
    debateRedisRepository.setExpireTime(
        roomId,
        RedisKeyUtil.getExpireKey(roomId)
    );
  }

  // 발언 타이머 내 발언 메서드
  public void submitSpeech(String roomId, DebateMessageDto dto) {

    // 1. 발언 타이머 expire 처리
    debateRedisRepository.expireTime(roomId, RedisKeyUtil.getExpireKey(roomId));
    String turnNo = debateRedisRepository.getRoomField(roomId, "turn");

    List<DebateMessageDto> speeches =
        debateRedisRepository.getSpeeches(RedisKeyUtil.getSpeechesKey(roomId), turnNo);
    speeches.add(dto);

    // 2. 발언 내용 추가
    debateRedisRepository.saveSpeeches(RedisKeyUtil.getSpeechesKey(roomId), turnNo, speeches);

  }

  // 청중 토론 종료 후 해당 메서드를 통해 nextUserId가 있는지 없는지 확인 후 startTurn을 진행할지, 다음 사람을 지정할지 결정
  private String getNextUserId(String roomId) {
    List<String> participants = debateRedisRepository.getParticipants(roomId);
    List<String> spokenUsers = debateRedisRepository.getSpokenUsers(roomId);

    if (spokenUsers.size() >= participants.size()) {
      System.out.println("Turn 종료");
      return null;
    }
    return participants.stream()
        .filter(p -> !spokenUsers.contains(p))
        .findFirst()
        .orElseGet(() -> {
          System.out.println("Turn 종료");
          return null;
        });
  }

  // 다음 발언자가 있으면 발언자 지정, 발언자가 없으면 다음 턴으로 넘어감
  public void startNextSpeaker(String roomId) {
    // 1. 발언 완료자에 추가
    List<String> spokenUsers =
        debateRedisRepository.getSpokenUsers(roomId);
    String userIdStr = debateRedisRepository.getRoomField(roomId, "currentSpeaker");

    if (!spokenUsers.contains(userIdStr)) {
      spokenUsers.add(userIdStr);
      debateRedisRepository.saveSpokenUsers(roomId, spokenUsers);
    }
    String nextUserId = getNextUserId(roomId);
    // 2. 다음 발언자 지정
    if (nextUserId != null) {
      // 2-1. 다음 발언자 지정
      debateRedisRepository.saveRoomField(roomId, "currentSpeaker", nextUserId);

      // 2-2. 발언 타이머 시작
      debateRedisRepository.setExpireTime(
          roomId,
          RedisKeyUtil.getExpireKey(roomId)
      );
    // 3. 다음 턴 시작
    } else {
      // 3-1. 다음 턴으로 변경
      debateRedisRepository.saveRoomField(roomId, "turn", String.valueOf((Integer.parseInt(debateRedisRepository.getRoomField(roomId, "turn")) + 1)));

      // 3-2. 다음 턴 시작
      startTurn(roomId, debateRedisRepository.getRoomField(roomId, "turn"));
    }
  }

}
