package com.likelion.realtalk.debate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveRequest {
    private Long roomId;
    private String userId;
}
