package com.likelion.realtalk.domain.debate.api;

import com.likelion.realtalk.domain.debate.dto.DebateTopicDto.CreateDebateTopicRequest;
import com.likelion.realtalk.domain.debate.dto.DebateTopicDto.DebateTopicResponse;
import com.likelion.realtalk.domain.debate.entity.DebateTopic;
import com.likelion.realtalk.domain.debate.service.DebateTopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/debate-topics")
@RequiredArgsConstructor
public class DebateTopicController {

    private final DebateTopicService service;

    // 1) 제목 추가
    @PostMapping
    public ResponseEntity<DebateTopicResponse> create(@Valid @RequestBody CreateDebateTopicRequest req) {
        DebateTopic saved = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/debate-topics/" + saved.getId()))
                .body(DebateTopicResponse.of(saved));
    }

    // 2) 전체 조회
    @GetMapping
    public List<DebateTopicResponse> list() {
        return service.findAll().stream()
                .map(DebateTopicResponse::of)
                .toList();
    }

    // 3) 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
