package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.DebateResultDto;
import com.likelion.realtalk.domain.debate.service.DebateResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debate-results")
public class DebateResultController {

  private final DebateResultService debateResultService;

  @GetMapping("/{roomUUID}")
  public ResponseEntity<DebateResultDto> getDebateResult(@PathVariable String roomUUID) {
    return ResponseEntity.ok(debateResultService.getDebateResult(roomUUID));
  }
}