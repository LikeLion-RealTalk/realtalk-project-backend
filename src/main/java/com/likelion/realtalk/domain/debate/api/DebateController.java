package com.likelion.realtalk.domain.debate.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

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
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import com.likelion.realtalk.global.security.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/debate-rooms")
public class DebateController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final DebateRoomService debateRoomService;
    private final ParticipantService participantService;
    private final RoomIdMappingService mapping;
    private final JwtProvider jwtProvider;

    @PostMapping("/api/auth/token")
    public Map<String, Object> issueToken(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String access = jwtProvider.createToken(user, 30 * 60 * 1000L); // 30분
        return Map.of("accessToken", access);
    }

    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        messagingTemplate.convertAndSend("/sub/debate-room/" + message.getRoomId(), message);
    }

    @MessageMapping("/debate/join")
    public void join(JoinRequest req, SimpMessageHeaderAccessor header) {
        String sessionId = header.getSessionId();
        UUID roomUuid = req.getRoomId();
        Long pk = mapping.toPk(roomUuid);

        // CONNECT 인터셉터에서 Authentication.setUser(...) 세팅되어 있어야 함
        var auth = (org.springframework.security.core.Authentication) header.getUser();
        var principal = (com.likelion.realtalk.domain.debate.auth.RoomPrincipal) auth.getPrincipal();

        boolean wantsSpeaker = "SPEAKER".equalsIgnoreCase(req.getRole());
        if (wantsSpeaker && !principal.isAuthenticated()) {
            messagingTemplate.convertAndSend(
                "/sub/debate-room/" + roomUuid,
                Map.of("type","JOIN_REJECTED","reason","auth_required","role", req.getRole())
            );
            return;
        }

        String subjectId = principal.isAuthenticated()
                ? ("user:" + principal.getUserId())
                : ("guest:" + sessionId);

        boolean ok = participantService.tryAddUserToRoomByPk(
            pk,
            subjectId,
            sessionId,
            req.getRole(),
            req.getSide(),
            principal.getDisplayName(),
            principal.isAuthenticated(),
            principal.getUserId() // 로그인 사용자면 Long, 게스트면 null
        );

        if (!ok) {
            messagingTemplate.convertAndSend(
                "/sub/debate-room/" + roomUuid,
                Map.of("type","JOIN_REJECTED","reason","capacity_or_status","role", req.getRole())
            );
            return;
        }

        // (선택) 성공 알림
        messagingTemplate.convertAndSend(
            "/sub/debate-room/" + roomUuid,
            Map.of("type","JOIN_ACCEPTED",
                  "userId", principal.isAuthenticated() ? principal.getUserId() : null,
                  "userName", principal.getDisplayName(),
                  "role", req.getRole(),
                  "side", req.getSide())
        );
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
