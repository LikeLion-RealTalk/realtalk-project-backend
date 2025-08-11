package com.likelion.realtalk.domain.debate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name="DEBATE_RESULT")
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class DebateResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "result_id")
  private Long id;

  // TODO. fk 연결 필요
  private Long roomId;

  @Column(columnDefinition = "TEXT", name = "ai_summary")
  private String aiSummary;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Builder
  public DebateResult(Long id, Long roomId, String aiSummary, LocalDateTime closedAt) {
    this.id = id;
    this.roomId = roomId;
    this.aiSummary = aiSummary;
    this.closedAt = closedAt;
  }
}