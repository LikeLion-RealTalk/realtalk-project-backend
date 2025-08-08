package com.likelion.realtalk.debate.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.likelion.realtalk.debate.entity.DebateRoom;

@Repository
public interface DebateRoomRepository extends JpaRepository<DebateRoom, UUID> {
}
