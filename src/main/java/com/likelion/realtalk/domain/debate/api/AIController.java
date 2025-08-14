package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.AiSummaryDto;
import com.likelion.realtalk.domain.debate.service.AiService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debate/{roomUUID}/ai")
public class AIController {

  private final AiService aiService;

  @GetMapping("/summaries")
  public ResponseEntity<List<AiSummaryDto>> getAiSummaries(@PathVariable String roomUUID) {
    return ResponseEntity.ok(aiService.getAiSummaries(roomUUID));
  }
}