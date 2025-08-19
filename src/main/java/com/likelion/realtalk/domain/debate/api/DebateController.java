package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.DebateRoomMatchRequest;
import com.likelion.realtalk.domain.debate.dto.DebatestartResponse;
import com.likelion.realtalk.domain.debate.dto.DebateRoomTimerDto;
import com.likelion.realtalk.domain.debate.service.DebateRoomMatchService;
import com.likelion.realtalk.domain.debate.service.SpeakerService;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
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
import com.likelion.realtalk.domain.debate.service.RedisRoomTracker;
import com.likelion.realtalk.domain.debate.dto.RoomUserInfo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debate-rooms")
public class DebateController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final DebateRoomService debateRoomService;
    private final ParticipantService participantService;
    private final RoomIdMappingService mapping;
    private final RedisRoomTracker redisRoomTracker;
    private final SpeakerService speakerService;
    private final DebateRoomMatchService debateRoomMatchService;

    @MessageMapping("/chat/message")
    public void message(ChatMessage incoming, SimpMessageHeaderAccessor headers) {
        String sessionId = headers.getSessionId();

        // 1) UUID -> PK 매핑 (UUID만 넘어오는 구조라면)
        UUID roomUuid = UUID.fromString(incoming.getRoomId());
        Long roomPk = mapping.toPk(roomUuid);

        // 2) 세션 기준으로 방 내 사용자 정보 조회 (예: Redis에 저장해둔 RoomUserInfo)
        RoomUserInfo ui = redisRoomTracker.findBySession(roomPk, sessionId);

        // 3) 서버가 브로드캐스트용 payload 구성 (클라 sender는 무시)
        ChatMessage out = new ChatMessage();
        out.setRoomId(incoming.getRoomId());
        out.setMessage(incoming.getMessage());
        out.setType("CHAT");
        out.setTimestamp(System.currentTimeMillis());

        if (ui != null) {
            out.setUserName(ui.getUserName());
            out.setUserId(ui.getUserId());
            out.setRole(ui.getRole());
            out.setSide(ui.getSide());
        } else {
            // 세션으로 못 찾았을 때의 안전장치: 인증 주체나 세션 꼬리표로 표기
            Principal p = headers.getUser();
            String fallback =
                (p != null) ? p.getName()
                            : ("guest:" + (sessionId != null && sessionId.length() > 6
                                        ? sessionId.substring(sessionId.length()-6)
                                        : "unknown"));
            out.setUserName(fallback);
            out.setUserId(null);
            out.setRole("AUDIENCE");
            out.setSide("A");
        }

        messagingTemplate.convertAndSend("/sub/debate-room/" + incoming.getRoomId(), out);
    }

    @MessageMapping("/debate/join")
    public void join(JoinRequest req, SimpMessageHeaderAccessor header) {
        UUID roomUuid = req.getRoomId();
        String sessionId = header.getSessionId();

        // 1) Principal 꺼내기 (RoomPrincipal 기준으로 인증 판정)
        Object userObj = header.getUser();
        org.springframework.security.core.Authentication auth =
            (userObj instanceof org.springframework.security.core.Authentication)
                ? (org.springframework.security.core.Authentication) userObj : null;

        com.likelion.realtalk.domain.debate.auth.RoomPrincipal rp =
            (auth != null && auth.getPrincipal() instanceof com.likelion.realtalk.domain.debate.auth.RoomPrincipal)
                ? (com.likelion.realtalk.domain.debate.auth.RoomPrincipal) auth.getPrincipal()
                : null;

        boolean isAuth = rp != null && rp.isAuthenticated() && rp.getUserId() != null;
        System.out.println("[JOIN][DBG] principalClass=" + (rp==null? "null" : rp.getClass().getSimpleName())
        + ", rp.isAuthenticated=" + (rp!=null && rp.isAuthenticated())
        + ", rp.userId=" + (rp!=null ? rp.getUserId() : null)
        + ", computed.isAuth=" + isAuth);
        Long uid       = (rp != null) ? rp.getUserId() : null;
        String name    = (rp != null) ? rp.getDisplayName() : null;

        // 2) SPEAKER는 로그인(인증) 필수 / AUDIENCE는 선택
        boolean wantsSpeaker = "SPEAKER".equalsIgnoreCase(req.getRole());
        if (wantsSpeaker && !isAuth) {
            System.out.println("wantsSpeaker: " + wantsSpeaker + " isAuth: "+ isAuth);
            String reason = (rp == null || rp instanceof com.likelion.realtalk.domain.debate.auth.GuestPrincipal || uid == null)
                            ? "auth_required" : "auth_broken";
            var rej = new java.util.HashMap<String,Object>();
            rej.put("type","JOIN_REJECTED");
            rej.put("reason", reason);
            rej.put("role", req.getRole());
            messagingTemplate.convertAndSend("/sub/debate-room/" + roomUuid, rej);
            return;
        }

        // 3) UUID -> PK 매핑
        Long pk;
        try {
            pk = mapping.toPk(roomUuid);
            log.info("[입장] 방 UUID={} → PK={}", roomUuid, pk);
        } catch (Exception e) {
            var rej = new java.util.HashMap<String,Object>();
            rej.put("type","JOIN_REJECTED");
            rej.put("reason","invalid_room");
            messagingTemplate.convertAndSend("/sub/debate-room/" + roomUuid, rej);
            return;
        }

        // 4) 표시명/주체 구성 (널/길이 안전)
        String safeGuest   = "게스트 " + (sessionId != null ? sessionId.substring(0, Math.min(6, sessionId.length())) : "UNKNOWN");
        String displayName = isAuth
            ? ((name != null && !name.isBlank()) ? name : ("User-" + uid))
            : safeGuest;
        String subjectId   = isAuth ? ("userId:" + uid) : ("guest:" + sessionId);
        Long   userIdOrNull = isAuth ? uid : null;

        log.info("[입장] 참가 시도: PK={}, 주체={}, 역할={}, 사이드={}, 인증여부={}, 표시명={}",
                pk, subjectId, req.getRole(), req.getSide(), isAuth, displayName);

        // 5) 참가 처리 (Redis 반영 포함)
        boolean ok;
        try {
            ok = participantService.tryAddUserToRoomByPk(
                    pk, subjectId, sessionId, req.getRole(), req.getSide(),
                    displayName, isAuth, userIdOrNull);
        } catch (Exception svcEx) {
            log.error("[입장] 참가 처리 중 예외 발생: {}", svcEx.toString(), svcEx);
            var rej = new java.util.HashMap<String,Object>();
            rej.put("type","JOIN_REJECTED");
            rej.put("reason","server_error");
            messagingTemplate.convertAndSend("/sub/debate-room/" + roomUuid, rej);
            return;
        }

        if (!ok) {
            var rej = new java.util.HashMap<String,Object>();
            rej.put("type","JOIN_REJECTED");
            rej.put("reason","capacity_or_status");
            rej.put("role", req.getRole());
            rej.put("nonce", req.getNonce()); // ★ 추가 (프론트 매칭용)
            messagingTemplate.convertAndSend("/sub/debate-room/" + roomUuid, rej);
            return;
        }

        // 6) 성공 브로드캐스트 (HashMap: null 허용)
        var acc = new java.util.HashMap<String,Object>();
        acc.put("type","JOIN_ACCEPTED");
        acc.put("userName", displayName);
        acc.put("role", req.getRole());
        acc.put("side", req.getSide());
        acc.put("nonce", req.getNonce()); // ★ 추가
        acc.put("subjectId", subjectId);
        if (userIdOrNull != null) acc.put("userId", userIdOrNull);

        messagingTemplate.convertAndSend("/sub/debate-room/" + roomUuid, acc);

        // 발언 시간 전달
        messagingTemplate.convertAndSend("/topic/speaker/" + roomUuid + "/expire", speakerService.getSpeakerExpire(roomUuid.toString()));

        // 전체 토론 시간 전달
        messagingTemplate.convertAndSend("/topic/debate/" + roomUuid + "/expire", debateRoomService.getDebateRoomExpireTime(roomUuid.toString()));

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
        participantService.broadcastParticipantsSpeaker(pk); // <- Speaker만 조회
        // participantService.broadcastParticipants(pk); // <- 접근 가능하게 public으로 변경
        return ResponseEntity.ok().build();
    }

    @GetMapping("/broadcast")
    @ResponseBody
    public ResponseEntity<Void> broadcastAllParticipants() {
        participantService.broadcastAllRooms();
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<DebateRoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        DebateRoom room = debateRoomService.createRoom(request);

        // PK(Long) -> UUID (Redis)
        UUID roomUuid = mapping.toUuid(room.getRoomId()); // room.getRoomId()는 PK(Long)

        DebateRoomResponse resp = DebateRoomResponse.from(room, roomUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
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

    @GetMapping("/{roomUUID}/expire")
    public ResponseEntity<DebateRoomTimerDto> getDebateRoomExpireTime(@PathVariable String roomUUID) {
        return ResponseEntity.ok(debateRoomService.getDebateRoomExpireTime(roomUUID));
    }

    //status, at 변경해야하므로 POST
    @PostMapping("/{roomUUID}/start")
    public ResponseEntity<DebatestartResponse> startRoom(@PathVariable UUID roomUUID) {
        Long pk = mapping.toPk(roomUUID);                  // 외부 UUID → 내부 PK
        DebatestartResponse updated = debateRoomService.startRoom(pk,roomUUID);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/match")
    public ResponseEntity<DebateRoomResponse> match(@RequestBody DebateRoomMatchRequest request) {
        DebateRoomResponse response = debateRoomMatchService.matchOne(request.categoryId());
        return ResponseEntity.ok(response);
    }
}
