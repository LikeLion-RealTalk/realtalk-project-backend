package com.likelion.realtalk.debate.dto;

import com.likelion.realtalk.debate.entity.DebateRoom;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomRequest {
    private Long userId;
    private Long roomId; //토론방 Id
    private String title; //토론 주제
    private String Status; //토론방 상태
    private String debateDescription; //토론 설명
    private CategoryDto category;

    private String sideA; //토론 사이드
    private String sideB; //토론 사이드

    private DebateRoom.DebateType debateType; //토론 방식
    private Long durationSeconds; //토론 시간
    private int maxSpeaker; //최대 발언자 수
    private int maxAudience; //최대 청중 수

    private int currentSpeaker;
    private int currentAudience;
    private Long elapsedSeconds;
    

    @Setter
    @Getter
    public static class CategoryDto{
        private Long id; //카테고리 Id
        private String name; // 카테고리 이름
    }
}
