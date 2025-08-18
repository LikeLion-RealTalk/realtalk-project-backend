package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerTimerDto;
import com.likelion.realtalk.domain.debate.service.SpeakerService;
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

}