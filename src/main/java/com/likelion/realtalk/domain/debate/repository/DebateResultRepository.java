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
                  COALESCE(COUNT(CASE WHEN dp.side = 'A' THEN 1 END) * 1.0 / COUNT(dp.side), 0),
                  COALESCE(COUNT(CASE WHEN dp.side = 'B' THEN 1 END) * 1.0 / COUNT(dp.side), 0),
                  COUNT(dp.side),
                  room.sideA,
                  room.sideB,
                  dr.aiSummary,
                  room.durationSeconds
                )
                FROM DebateResult dr
                LEFT JOIN dr.debateRoom room
                LEFT JOIN room.category c
                LEFT JOIN DebateParticipant dp ON dp.debateRoom = room
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