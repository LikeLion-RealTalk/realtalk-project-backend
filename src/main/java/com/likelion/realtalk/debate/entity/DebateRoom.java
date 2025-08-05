package com.likelion.realtalk.debate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class DebateRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "user_id")
    private Long userId;

    private String title; //토론 주제

    @Column(name = "debate_description", columnDefinition = "TEXT")
    private String debateDescription; // 토론 설명

    @Column(name = "category_id")
    private Long categoryId; //카테고리

    @Column(name = "side_a")
    private String sideA; //토론 사이드

    @Column(name = "side_b")
    private String sideB; //토론 사이드

    public enum DebateType { //토론 방식
        NORMAL, FAST
    }

    @Column(name = "duration_seconds")
    private Long durationSeconds; //토론 시간

    @Column(name = "max_active_speakers")
    private Long maxSpeaker; //최대 발언자 수

    @Column(name = "max_listeners")
    private Long maxAudience; //최대 청중 수

    @Enumerated(EnumType.STRING)
    @Column(name = "debate_type")
    private DebateType debateType;

    @Enumerated(EnumType.STRING)
    private DebateRoomStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Lob
    @Column(name = "ai_summary")
    private String aiSummary;


    @Column(name = "max_participants")
    private Long maxParticipants;
}
