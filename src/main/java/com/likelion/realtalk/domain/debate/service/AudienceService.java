package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.dto.AudienceTimerDto;
import com.likelion.realtalk.domain.debate.repository.DebateRedisRepository;
import com.likelion.realtalk.global.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AudienceService {

  private final DebateRedisRepository debateRedisRepository;
  private final SimpMessagingTemplate messagingTemplate;

  public AudienceTimerDto getAudienceExpire(String roomUUID) {
    return AudienceTimerDto
        .builder()
        .audienceExpireTime(debateRedisRepository.getAudienceExpire(roomUUID))
        .build();
  }

  // 청중 시간 타이머 발행
  public void pubAudienceExpireTimer(String roomUUID)  {
    String expireTime = debateRedisRepository.setExpireTime(roomUUID, RedisKeyUtil.getAudienceExpireKey(roomUUID));
    AudienceTimerDto audienceTimerDto = AudienceTimerDto.builder().audienceExpireTime(expireTime).build();
    messagingTemplate.convertAndSend("/topic/audience/" + roomUUID +"/expire", audienceTimerDto);
  }

  // redis 정보 삭제
  public void clearAudienceCaches(String roomUUID) {
    this.debateRedisRepository.deleteByKey(RedisKeyUtil.getAudienceExpireKey(roomUUID));
  }
}