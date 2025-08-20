package com.likelion.realtalk.domain.debate.repository;

import com.likelion.realtalk.domain.debate.entity.DebateTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebateTopicRepository extends JpaRepository<DebateTopic, Long> {
    boolean existsByTitle(String title);
}
