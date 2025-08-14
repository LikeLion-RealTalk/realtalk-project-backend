package com.likelion.realtalk.domain.debate.auth;

public class GuestPrincipal implements RoomPrincipal {
    private final String guestId;    // "guest-AB12CD"
    private final String guestName;  // "게스트 AB12CD"

    public GuestPrincipal(String guestId, String guestName) {
        this.guestId = guestId;
        this.guestName = guestName;
    }

    @Override public boolean isAuthenticated() { return false; }
    @Override public Long getUserId() { return null; }
    @Override public String getName() { return guestId; }
    @Override public String getDisplayName() { return guestName; }
}
