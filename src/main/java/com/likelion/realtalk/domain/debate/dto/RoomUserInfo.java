package com.likelion.realtalk.domain.debate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUserInfo {
    private String sessionId;      // Redis 해시 field (세션 기준)
    private String subjectId;      // "user:123" 또는 "guest:{sessionId}"
    private Long   userId;         // 로그인 사용자 PK, 게스트는 null
    private String userName;       // 화면에 보여줄 이름 (회원명/게스트명)
    private String role;           // SPEAKER / AUDIENCE
    private String side;           // A / B
    private boolean authenticated; // 로그인 여부
    // (옵션) private Long joinedAt; // epoch seconds 저장했다면 추가
}
