package com.likelion.realtalk.domain.debate.websocket;

import com.likelion.realtalk.domain.debate.dto.RoomUserInfo;
import com.likelion.realtalk.domain.debate.service.ParticipantService;
import com.likelion.realtalk.domain.debate.service.RedisRoomTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {

    private final RedisRoomTracker redisRoomTracker;
    private final ParticipantService participantService;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) return;

        // 1) 모든 방 PK 스캔
        Set<Long> pks = redisRoomTracker.getAllRoomPks();
        if (pks == null || pks.isEmpty()) return;

        for (Long pk : pks) {
            // 2) 이 방에 해당 세션이 있는지 확인
            RoomUserInfo info = redisRoomTracker.findBySession(pk, sessionId);
            if (info == null) continue;

            String subjectId = info.getSubjectId();

            // 3) Redis에서 세션/참가자 정리
            redisRoomTracker.removeSession(pk, sessionId);

            // 4) 메모리/브로드캐스트 정리
            participantService.removeUserFromRoom(pk, subjectId);
            participantService.broadcastParticipantsSpeaker(pk);
            participantService.broadcastAllRooms();

            log.debug("Disconnected cleanup: pk={}, sessionId={}, subjectId={}", pk, sessionId, subjectId);
            break; // 찾았으면 종료
        }
    }
}
