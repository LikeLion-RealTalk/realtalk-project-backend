package com.likelion.realtalk.domain.debate.repository;

import com.likelion.realtalk.domain.debate.entity.DebateRoomStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.likelion.realtalk.domain.debate.entity.DebateRoom;

@Repository
public interface DebateRoomRepository extends JpaRepository<DebateRoom, Long> {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
        update DebateRoom r
           set r.status = :started, r.startedAt = :now
         where r.roomId = :id and r.status = :waiting
    """)
  int startIfWaiting(@Param("id") Long id,
      @Param("now") LocalDateTime now,
      @Param("started") DebateRoomStatus started,
      @Param("waiting") DebateRoomStatus waiting);

  @Query("SELECT r FROM DebateRoom r join fetch r.category")
  List<DebateRoom> findAllWithCategory();
}
