package com.likelion.realtalk.debate.controller;

import java.util.Collection;
import java.util.List;
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

import com.likelion.realtalk.debate.dto.AiSummaryResponse;
import com.likelion.realtalk.debate.dto.ChatMessage;
import com.likelion.realtalk.debate.dto.CreateRoomRequest;
import com.likelion.realtalk.debate.dto.DebateRoomResponse;
import com.likelion.realtalk.debate.dto.JoinRequest;
import com.likelion.realtalk.debate.dto.LeaveRequest;
import com.likelion.realtalk.debate.dto.RoomUserInfo;
import com.likelion.realtalk.debate.entity.DebateRoom;
import com.likelion.realtalk.debate.service.DebateRoomService;
import com.likelion.realtalk.debate.service.ParticipantService;
import com.likelion.realtalk.debate.service.RoomIdMappingService;

import lombok.RequiredArgsConstructor;

@Controller
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
    public void join(JoinRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId(); // WebSocket 세션 ID
        UUID roomUuid = request.getRoomId();
        Long pk = mapping.toPk(roomUuid);

        // Long 시그니처 사용
        participantService.addUserToRoomByPk(pk, request.getUserId(), sessionId, request.getRole(), request.getSide());

        // 현재 참여자 목록 broadcast
        // ✅ RoomUserInfo 전체 정보 전송
        // 현재 참여자 목록 얻을 때도 Long 사용
        Collection<RoomUserInfo> participants = participantService.getDetailedUsersInRoom(pk);

        // 브로드캐스트는 토픽에 UUID 써야 하므로 기존 UUID를 사용
        messagingTemplate.convertAndSend("/sub/debate-room/" + roomUuid + "/participants", participants);
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
}
