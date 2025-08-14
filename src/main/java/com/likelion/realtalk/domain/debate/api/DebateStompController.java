package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.service.DebateRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class DebateStompController {

  private final DebateRoomService debateRoomService;

  @MessageMapping("/debate/extend")
  public void extendDebateRoomTime(String roomUUID) {
    debateRoomService.extendDebateTime(roomUUID);
  }
}