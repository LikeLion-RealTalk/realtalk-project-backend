package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.SideStatsDto;
import com.likelion.realtalk.domain.debate.dto.SideStatsRequest;
import com.likelion.realtalk.domain.debate.service.RoomIdMappingService;
import com.likelion.realtalk.domain.debate.service.SideStatsService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class SideStatsController {

    private final SideStatsService sideStatsService;
    private final RoomIdMappingService mapping;

    /** REST: 현재 퍼센트 즉시 조회 (초기 렌더용) */
    @GetMapping("/{roomId}/side-stats")
    public SideStatsDto getSideStats(@PathVariable UUID roomId) {
        Long pk = mapping.toPk(roomId);
        return sideStatsService.compute(pk);
    }

    /** STOMP: 클라이언트가 on-demand로 브로드캐스트 요청할 수 있게 */
    @MessageMapping("/debate/side-stats")
    public void pushSideStats(SideStatsRequest req) {
        Long pk = mapping.toPk(req.getRoomId());
        sideStatsService.broadcast(pk);
    }
}
