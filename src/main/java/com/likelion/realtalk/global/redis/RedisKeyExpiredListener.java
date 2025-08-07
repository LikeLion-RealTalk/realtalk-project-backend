package com.likelion.realtalk.global.redis;

import com.likelion.realtalk.domain.debate.repository.DebateRedisRepository;
import com.likelion.realtalk.domain.debate.service.DebateService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {

  private final DebateService debateService;
  private final DebateRedisRepository debateRedisRepository;

  public RedisKeyExpiredListener(RedisMessageListenerContainer listenerContainer,
      DebateService debateService, DebateRedisRepository debateRedisRepository) {
    super(listenerContainer);
    this.debateService = debateService;
    this.debateRedisRepository = debateRedisRepository;
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String expiredKey = message.toString();
    String roomId = expiredKey.split(":")[1];
    if (expiredKey.endsWith("currentSpeakerExpire")) {
      // 1. 청중 타이머 시작
      debateRedisRepository.setExpireTime(roomId, RedisKeyUtil.getAudienceExpireKey(roomId));
    } else if (expiredKey.endsWith("audienceExpire")) {

      // 의논 시간 종료 시 빈 문자열로 넘김
      debateService.startNextSpeaker(roomId);
    }
  }
}