package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.DebateMessageDto;
import com.likelion.realtalk.domain.debate.dto.DebateRoomDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerTimerDto;
import com.likelion.realtalk.domain.debate.service.AiService;
import com.likelion.realtalk.domain.debate.service.DebateResultService;
import com.likelion.realtalk.domain.debate.service.SpeakerService;
import com.likelion.realtalk.domain.debate.type.Side;
import io.netty.util.internal.ThreadLocalRandom;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/debate/{roomUUID}/speakers")
public class SpeakerController {

  private final SpeakerService speakerService;
  private final AiService aiService;
  private final DebateResultService  debateResultService;

  // 토론방 최초 입장 시 지난 발언 내용 조회 api
  @GetMapping
  public ResponseEntity<ArrayList<SpeakerMessageDto>> getSpeeches(@PathVariable String roomUUID) {
    return ResponseEntity.ok(speakerService.getSpeeches(roomUUID));
  }

  // 토론방 최초 입장 시 현재 발언 타이머 조회 api
  @GetMapping("/expire")
  public ResponseEntity<SpeakerTimerDto> getSpeakerExpire(@PathVariable String roomUUID) {
    return ResponseEntity.ok(speakerService.getSpeakerExpire(roomUUID));
  }

  @GetMapping("/add")
  public void addParticipant() {
    ArrayList<Long> userIds = new ArrayList<>();
    userIds.add(1L);
    userIds.add(2L);
    speakerService.setDebateRoom(new DebateRoomDto("1", 1L, userIds, "FAST"));
  }

  @GetMapping("/submit")
  public void submitSpeech() {
    speakerService.submitSpeech(new DebateMessageDto("1", 1L, "저는 무상교육에 반대합니다. 무상교육으로 인한 세금 부담이 커지기 때문입니다.", ThreadLocalRandom.current().nextBoolean() ? Side.A : Side.B));
  }

}