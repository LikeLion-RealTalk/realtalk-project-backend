package com.likelion.realtalk.debate.repository;

import com.likelion.realtalk.debate.model.DebateRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebateRoomRepository extends JpaRepository<DebateRoom, Long> {
}
