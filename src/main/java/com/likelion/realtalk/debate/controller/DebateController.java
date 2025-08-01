package com.example.demo.controller;

import com.example.demo.dto.ChatMessage;
import com.example.demo.dto.JoinRequest;

import com.example.demo.dto.CreateRoomRequest;
import com.example.demo.model.DebateRoom;
import com.example.demo.service.DebateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DebateController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final DebateService debateService;

    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        messagingTemplate.convertAndSend("/sub/debate-room/" + message.getRoomId(), message);
    }

    @MessageMapping("/debate/join")
    public void join(JoinRequest request) {
        debateService.handleJoin(request.getRoomId(), request.getUserId());
    }

    @PostMapping("/debate/rooms")
    @ResponseBody
    public ResponseEntity<DebateRoom> createRoom(@RequestBody CreateRoomRequest request) {
        DebateRoom room = debateService.createRoom(request);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/debate/rooms")
    @ResponseBody
    public ResponseEntity<List<DebateRoom>> getAllRooms() {
        return ResponseEntity.ok(debateService.findAllRooms());
    }
}
