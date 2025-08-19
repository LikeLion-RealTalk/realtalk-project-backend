
package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.ChangeSideRequest;
import com.likelion.realtalk.domain.debate.service.SideChangeService;
import com.likelion.realtalk.domain.debate.service.RoomIdMappingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debate-rooms")
@RequiredArgsConstructor
public class SideController {

  private final SideChangeService sideChangeService;
  private final RoomIdMappingService mapping;

  /** 사이드 변경 (A/B) */
  @PostMapping("/{roomId}/side")
  public ResponseEntity<?> changeSide(
      @PathVariable UUID roomId,
      @RequestBody ChangeSideRequest req
  ) {
    // PathVariable과 Body(roomId)가 다를 수 있으니 Path 기준으로 통일
    Long pk = mapping.toPk(roomId);
    sideChangeService.changeSideAndBroadcast(pk, req.getSubjectId(), req.getSide());
    return ResponseEntity.ok().body(
        java.util.Map.of("ok", true, "side", req.getSide())
    );
  }
}
