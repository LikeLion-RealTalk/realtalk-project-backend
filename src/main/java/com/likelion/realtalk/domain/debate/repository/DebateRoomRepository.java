package com.likelion.realtalk.domain.debate.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.likelion.realtalk.domain.debate.entity.DebateRoom;

@Repository
public interface DebateRoomRepository extends JpaRepository<DebateRoom, Long> {
}
