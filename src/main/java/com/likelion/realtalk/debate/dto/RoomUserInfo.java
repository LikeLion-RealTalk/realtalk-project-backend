package com.likelion.realtalk.debate.dto;

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
    private String userId;
    private String role; // SPEAKER, AUDIENCE
    private String side;  // A / B
}
