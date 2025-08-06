package com.likelion.realtalk.debate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRequest {
    private Long roomId;
    private String userId;
    private String role;
    private String side;
}
