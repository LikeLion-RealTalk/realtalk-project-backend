package com.likelion.realtalk.domain.debate.repository;

import com.likelion.realtalk.domain.debate.dto.DebateResultDto;
import com.likelion.realtalk.domain.debate.entity.DebateResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DebateResultRepository extends JpaRepository<DebateResult, Long> {

  @Query("""
                SELECT new com.likelion.realtalk.domain.debate.dto.DebateResultDto(
                  room.debateType,
                  room.title,
                  c.categoryName,
                  room.sideA,
                  room.sideB,
                  dr.aiSummary,
                  room.durationSeconds,
                  dr.sideARate,
                  dr.sideBRate,
                  dr.totalCount
                )
                FROM DebateResult dr
                LEFT JOIN dr.debateRoom room
                LEFT JOIN room.category c
                WHERE room.roomId = :roomId
                GROUP BY
                  room.debateType,
                  room.title,
                  c.categoryName,
                  room.sideA,
                  room.sideB,
                  room.startedAt,
                  room.closedAt,
                  dr.aiSummary
      """)
  DebateResultDto findDebateresultByDebateRoomId(Long roomId);
}