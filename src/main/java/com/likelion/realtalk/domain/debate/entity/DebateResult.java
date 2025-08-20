package com.likelion.realtalk.domain.debate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id")
  private DebateRoom debateRoom;

  @Column(columnDefinition = "TEXT", name = "ai_summary")
  private String aiSummary;

  @Column(name = "sid_a_rate")
  private int sideARate;

  @Column(name = "sid_b_rate")
  private int sideBRate;

  @Column(name = "total_count")
  private Long totalCount;

  @Builder
  public DebateResult(Long id, DebateRoom debateRoom, String aiSummary, int sideARate, int sideBRate, Long totalCount) {
    this.id = id;
    this.aiSummary = aiSummary;
    this.debateRoom = debateRoom;
    this.sideARate = sideARate;
    this.sideBRate = sideBRate;
    this.totalCount = totalCount;
  }

}