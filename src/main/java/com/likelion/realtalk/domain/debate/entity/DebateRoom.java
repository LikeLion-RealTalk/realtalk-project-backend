package com.likelion.realtalk.domain.debate.entity;

import com.likelion.realtalk.domain.category.entity.Category;
import com.likelion.realtalk.domain.debate.type.DebateType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter //TODO: 준표 왈 : @Setter 없애야함
@Entity
@NoArgsConstructor
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "side_a")
    private String sideA; //토론 사이드

    @Column(name = "side_b")
    private String sideB; //토론 사이드

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
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "max_participants")
    private Long maxParticipants;

    @Builder
    public DebateRoom(Long userId, String title, String debateDescription,
        Category category, String sideA, String sideB, Long durationSeconds, Long maxSpeaker,
        Long maxAudience, DebateType debateType, DebateRoomStatus status, LocalDateTime startedAt,
        LocalDateTime closedAt, Long maxParticipants) {
        this.userId = userId;
        this.title = title;
        this.debateDescription = debateDescription;
        this.category = category;
        this.sideA = sideA;
        this.sideB = sideB;
        this.durationSeconds = durationSeconds;
        this.maxSpeaker = maxSpeaker;
        this.maxAudience = maxAudience;
        this.debateType = debateType;
        this.status = status;
        this.startedAt = startedAt;
        this.closedAt = closedAt;
        this.maxParticipants = maxParticipants;
    }

    public void endDebate(LocalDateTime closedAt) {
        this.closedAt = closedAt;
        this.durationSeconds = Duration.between(this.startedAt, this.closedAt).toSeconds();
        this.status = DebateRoomStatus.ended;
    }
}
