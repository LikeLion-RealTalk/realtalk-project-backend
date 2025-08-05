package com.likelion.realtalk.debate.controller;

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

import com.likelion.realtalk.debate.dto.ChatMessage;
import com.likelion.realtalk.debate.dto.CreateRoomRequest;
import com.likelion.realtalk.debate.dto.DebateRoomResponse;
import com.likelion.realtalk.debate.dto.JoinRequest;
import com.likelion.realtalk.debate.dto.LeaveRequest;
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
        participantService.addUserToRoom(request.getRoomId(), request.getUserId(), sessionId);

        // 현재 참여자 목록 broadcast
        List<String> participants = participantService.getUsersInRoom(request.getRoomId());
        messagingTemplate.convertAndSend("/sub/debate-room/" + request.getRoomId() + "/participants", participants);
    }

    @MessageMapping("/debate/leave") 
    public void leave(LeaveRequest request){
        participantService.removeUserFromRoom(request.getRoomId(), request.getUserId());
    }

    @PostMapping("/debate/rooms")
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
    public ResponseEntity<DebateRoomResponse> getRoomSummary(@PathVariable Long roomId) {
        DebateRoomResponse response = debateRoomService.findRoomSummaryById(roomId);
        return ResponseEntity.ok(response);
    }
}
