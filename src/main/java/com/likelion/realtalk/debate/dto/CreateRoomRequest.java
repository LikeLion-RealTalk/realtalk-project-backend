package com.example.demo.dto;

import com.example.demo.model.DebateRoom;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomRequest {
    private Long categoryId;
    private Long userId;
    private String title;
    private DebateRoom.DebateType debateType;
    private Long durationSeconds;
    private String sideA;
    private String sideB;
    private Long maxParticipants;
}
