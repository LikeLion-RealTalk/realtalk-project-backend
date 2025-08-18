package com.likelion.realtalk.domain.debate.service;

import com.likelion.realtalk.domain.debate.repository.DebateRedisRepository;
import com.likelion.realtalk.global.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AudienceService {

  private final DebateRedisRepository debateRedisRepository;

  // 청중 시간 타이머 발행
  public void pubAudienceExpireTimer(String roomUUID)  {
    debateRedisRepository.setExpireTime(roomUUID, RedisKeyUtil.getAudienceExpireKey(roomUUID));
  }

  // redis 정보 삭제
  public void clearAudienceCaches(String roomUUID) {
    this.debateRedisRepository.deleteByKey(RedisKeyUtil.getAudienceExpireKey(roomUUID));
  }
}