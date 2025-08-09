package com.likelion.realtalk.debate.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter //TODO: 준표 왈 : @Setter 없애야함
@Entity
public class DebateRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId; // ← PK(Long)

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "title")
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

    @Column(name = "max_speaker")
    private Long maxSpeaker; //최대 발언자 수

    @Column(name = "max_audience")
    private Long maxAudience; //최대 청중 수

    @Enumerated(EnumType.STRING)
    @Column(name = "debate_type")
    private DebateType debateType;

    @Enumerated(EnumType.STRING)
    private DebateRoomStatus status;

    @Column(name = "started_at")
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Lob
    @Column(name = "ai_summary")
    private String aiSummary;


    @Column(name = "max_participants")
    private Long maxParticipants;
}
