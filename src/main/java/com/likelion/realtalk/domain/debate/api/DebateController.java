package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.DebatestartResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.likelion.realtalk.domain.debate.dto.AiSummaryResponse;
import com.likelion.realtalk.domain.debate.dto.ChatMessage;
import com.likelion.realtalk.domain.debate.dto.CreateRoomRequest;
import com.likelion.realtalk.domain.debate.dto.DebateRoomResponse;
import com.likelion.realtalk.domain.debate.dto.JoinRequest;
import com.likelion.realtalk.domain.debate.dto.LeaveRequest;
import com.likelion.realtalk.domain.debate.entity.DebateRoom;
import com.likelion.realtalk.domain.debate.service.DebateRoomService;
import com.likelion.realtalk.domain.debate.service.ParticipantService;
import com.likelion.realtalk.domain.debate.service.RoomIdMappingService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debate-rooms")
public class DebateController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final DebateRoomService debateRoomService;
    private final ParticipantService participantService;
    private final RoomIdMappingService mapping;

    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        messagingTemplate.convertAndSend("/sub/debate-room/" + message.getRoomId(), message);
    }

    @MessageMapping("/debate/join")
    public void join(JoinRequest request, SimpMessageHeaderAccessor header) {
        String sessionId = header.getSessionId();
        UUID roomUuid = request.getRoomId();
        Long pk = mapping.toPk(roomUuid);

        boolean ok = participantService.tryAddUserToRoomByPk(
            pk, request.getUserId(), sessionId, request.getRole(), request.getSide()
        );

        if (!ok) {
            messagingTemplate.convertAndSend(
                "/sub/debate-room/" + roomUuid,
                Map.of("type","JOIN_REJECTED",
                    "role", request.getRole(),
                    "reason", "capacity_or_status")
            );
            return;
        }

        // 성공 시: (broadcastParticipants가 내부에서 이미 진행됨)
    }

    @MessageMapping("/debate/leave") 
    public void leave(LeaveRequest request){
        Long pk = mapping.toPk(request.getRoomId());
        participantService.removeUserFromRoom(pk, request.getUserId());
    }

    @GetMapping("/{roomId}/broadcast")
    @ResponseBody
    public ResponseEntity<Void> broadcastRoomParticipants(@PathVariable UUID roomId) {
        Long pk = mapping.toPk(roomId);
        participantService.broadcastParticipants(pk); // <- 접근 가능하게 public으로 변경
        return ResponseEntity.ok().build();
    }

    @GetMapping("/broadcast")
    @ResponseBody
    public ResponseEntity<Void> broadcastAllParticipants() {
        participantService.broadcastAllRooms();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/")
    @ResponseBody
    public ResponseEntity<DebateRoom> createRoom(@RequestBody CreateRoomRequest request) {
        DebateRoom room = debateRoomService.createRoom(request);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<List<DebateRoomResponse>> getAllRooms() {
        return ResponseEntity.ok(debateRoomService.findAllRooms());
    }

    @GetMapping("/{roomId}")
    @ResponseBody
    public ResponseEntity<DebateRoomResponse> getRoomById(@PathVariable UUID roomId) {
       // DebateRoomService가 Long PK 시그니처면 변환해서 호출
        DebateRoomResponse response = debateRoomService.findRoomById(roomId); // ← 서비스가 UUID 받도록 되어있으면 그대로 사용
        // 만약 서비스가 Long만 받게 바꿨다면:
        // Long pk = mapping.toPk(roomId);
        // DebateRoomResponse response = debateRoomService.findRoomById(pk);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/summary/{roomId}")
    @ResponseBody
    public ResponseEntity<AiSummaryResponse> getRoomSummaryById(@PathVariable UUID roomId) {
        // 만약 서비스가 Long만 받게 바꿨다면:
        Long pk = mapping.toPk(roomId);
        AiSummaryResponse response = debateRoomService.findAiSummaryById(pk);
        // AiSummaryResponse response = debateRoomService.findAiSummaryById(roomId);
        return ResponseEntity.ok(response);
    }

    //status, at 변경해야하므로 POST
    @PostMapping("/{roomUUID}/start")
    public ResponseEntity<DebatestartResponse> startRoom(@PathVariable UUID roomUUID) {
        Long pk = mapping.toPk(roomUUID);                  // 외부 UUID → 내부 PK
        DebatestartResponse updated = debateRoomService.startRoom(pk,roomUUID);
        return ResponseEntity.ok(updated);
    }
}
