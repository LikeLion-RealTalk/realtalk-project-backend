package com.likelion.realtalk.global.redis;

import com.likelion.realtalk.domain.debate.service.SpeakerService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {

  private final SpeakerService debateService;

  public RedisKeyExpiredListener(RedisMessageListenerContainer listenerContainer,
      SpeakerService debateService) {
    super(listenerContainer);
    this.debateService = debateService;
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String expiredKey = message.toString();
    String roomUUID = expiredKey.split(":")[1];
    if (expiredKey.endsWith("currentSpeakerExpire")) {
      // 1. 청중 타이머 시작
      debateService.pubAudienceExpireTimer(roomUUID);
    } else if (expiredKey.endsWith("audienceExpire")) {
      // 의논 시간 종료 시 빈 문자열로 넘김
      debateService.startNextSpeaker(roomUUID);
    }
  }
}