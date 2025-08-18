package com.likelion.realtalk.domain.debate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {
    private String roomId;     // UUID 문자열
    private String message;    // 내용

    // 서버가 채워서 내려줄 메타들
    private String userName;   // 표시명
    private Long   userId;     // 로그인 사용자 PK (게스트면 null)
    private String role;       // SPEAKER / AUDIENCE
    private String side;       // A / B
    private String type;       // "CHAT" | "START" | "FINISH" 등
    private long   timestamp;  // epoch millis
}
