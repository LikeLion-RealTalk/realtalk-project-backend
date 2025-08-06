package com.likelion.realtalk.debate.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebateParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participantId")
    private Long participantId;

    // 💬 DebateRoom과 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private DebateRoom debateRoom;

    // 유저 ID (회원일 경우)
    @Column(name = "userId")
    private Long userId;

    // 게스트 ID (비회원일 경우)
    @Column(name = "guestId")
    private Long guestId;

    // 발언자/청중 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    // 찬반 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private Side side;

    // 입장 시각
    @Column(name = "joinedAt")
    private LocalDateTime joinedAt;

    // ENUM 정의
    public enum Role {
        SPEAKER, AUDIENCE
    }

    public enum Side {
        A, B
    }
}
