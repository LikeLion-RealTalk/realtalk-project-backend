package com.likelion.realtalk.domain.debate.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import com.likelion.realtalk.domain.debate.dto.DebateTopicDto.CreateDebateTopicRequest;
import com.likelion.realtalk.domain.debate.entity.DebateTopic;
import com.likelion.realtalk.domain.debate.repository.DebateTopicRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DebateTopicService {

    private final DebateTopicRepository repository;

    @Transactional
    public DebateTopic create(CreateDebateTopicRequest req) {
        if (repository.existsByTitle(req.title())) {
            throw new IllegalArgumentException("이미 존재하는 토론 주제입니다: " + req.title());
        }
        DebateTopic topic = DebateTopic.builder()
                .title(req.title())
                .build();
        return repository.save(topic);
    }

public List<DebateTopic> findAll() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "id")); // id 오름차순
}

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 토론 주제입니다: " + id);
        }
        repository.deleteById(id);
    }
}
