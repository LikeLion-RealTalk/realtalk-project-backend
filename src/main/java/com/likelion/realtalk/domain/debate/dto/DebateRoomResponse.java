package com.likelion.realtalk.domain.debate.dto;

import com.likelion.realtalk.domain.debate.type.DebateType;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DebateRoomResponse {

    private UUID roomId;
    private String title;
    private String status;

    private CategoryDto category;

    private String sideA;
    private String sideB;

    private Long maxSpeaker;
    private Long maxAudience;

    private DebateType debateType;

    private Long currentSpeaker;
    private Long currentAudience;

    private Long elapsedSeconds; // 현재까지 진행된 초

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class CategoryDto {
        private Long id;
        private String name;
    }
}
