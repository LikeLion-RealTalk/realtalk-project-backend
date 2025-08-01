package com.example.demo.model;

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

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "user_id")
    private Long userId;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "debate_type")
    private DebateType debateType;

    @Enumerated(EnumType.STRING)
    private DebateRoomStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Lob
    @Column(name = "ai_summary")
    private String aiSummary;

    @Column(name = "side_a")
    private String sideA;

    @Column(name = "side_b")
    private String sideB;

    @Column(name = "max_participants")
    private Long maxParticipants;

    public enum DebateType {
        NORMAL, FAST
    }

   
}
