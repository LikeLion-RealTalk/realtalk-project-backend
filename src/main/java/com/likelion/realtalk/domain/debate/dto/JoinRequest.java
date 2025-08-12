package com.likelion.realtalk.domain.debate.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRequest {
    private UUID roomId;
    // private String userId;
    private String role;
    private String side;
}
