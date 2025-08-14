package com.likelion.realtalk.domain.debate.auth;

import java.security.Principal;

public interface RoomPrincipal extends Principal {
    boolean isAuthenticated();     // 로그인 사용자면 true, 게스트면 false
    Long getUserId();              // 게스트면 null
    String getDisplayName();       // userName 또는 guestName
}
