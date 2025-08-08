package com.likelion.realtalk.domain.debate.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.likelion.realtalk.domain.debate.dto.DebateMessageDto;
import com.likelion.realtalk.domain.debate.dto.DebateRoomDto;
import com.likelion.realtalk.domain.debate.service.DebateService;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/test")
public class DebateController {

  private final DebateService debateService;

  @GetMapping("/add")
  public void addParticipant() {
    ArrayList<Long> userIds = new ArrayList<>();
    userIds.add(1L);
    userIds.add(2L);
//    debateService.setDebateRoom(new DebateRoomDto(1L, userIds, "FAST"));
  }

  @GetMapping("/start")
  public void startTurn() throws JsonProcessingException {
    debateService.startTurn("1", "1");
  }

  @GetMapping("/submit")
  public void submitSpeech() {
    debateService.submitSpeech("1", new DebateMessageDto(1L, "test"));
  }
}