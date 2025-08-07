package com.likelion.realtalk.debate.controller;

import java.util.Collection;
import java.util.List;

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

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/debate-rooms")
public class DebateController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final DebateRoomService debateRoomService;
    private final ParticipantService participantService;

    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        messagingTemplate.convertAndSend("/sub/debate-room/" + message.getRoomId(), message);
    }

    @MessageMapping("/debate/join")
    public void join(JoinRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId(); // WebSocket 세션 ID
        participantService.addUserToRoom(request.getRoomId(), request.getUserId(), sessionId, request.getRole(), request.getSide());

        // 현재 참여자 목록 broadcast
        // ✅ RoomUserInfo 전체 정보 전송
        Collection<RoomUserInfo> participants = participantService.getDetailedUsersInRoom(request.getRoomId());
        messagingTemplate.convertAndSend("/sub/debate-room/" + request.getRoomId() + "/participants", participants);
    }

    @MessageMapping("/debate/leave") 
    public void leave(LeaveRequest request){
        participantService.removeUserFromRoom(request.getRoomId(), request.getUserId());
    }

    @GetMapping("/{roomId}/broadcast")
    @ResponseBody
    public ResponseEntity<Void> broadcastRoomParticipants(@PathVariable Long roomId) {
        participantService.broadcastParticipants(roomId); // <- 접근 가능하게 public으로 변경
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
    public ResponseEntity<DebateRoomResponse> getRoomById(@PathVariable Long roomId) {
        DebateRoomResponse response = debateRoomService.findRoomById(roomId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/summary/{roomId}")
    @ResponseBody
    public ResponseEntity<AiSummaryResponse> getRoomSummaryById(@PathVariable Long roomId) {
        AiSummaryResponse response = debateRoomService.findAiSummaryById(roomId);
        return ResponseEntity.ok(response);
    }
}
