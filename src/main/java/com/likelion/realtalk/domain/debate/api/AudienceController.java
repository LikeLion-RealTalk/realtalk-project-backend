package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.AudienceTimerDto;
import com.likelion.realtalk.domain.debate.service.AudienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debate/{roomUUID}/audiences")
public class AudienceController {

  private final AudienceService audienceService;

  // 토론방 최초 입장 시 청중 타이머 조회 api
  @GetMapping("/expire")
  public ResponseEntity<AudienceTimerDto> getAudienceExpire(@PathVariable String roomUUID) {
    return ResponseEntity.ok(audienceService.getAudienceExpire(roomUUID));
  }
}