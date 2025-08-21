package com.likelion.realtalk.domain.debate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DebateTopicDto {

    // 생성 요청 DTO
    public record CreateDebateTopicRequest(
            @NotBlank @Size(max = 200) String title
    ) {}

    // 응답 DTO
    public record DebateTopicResponse(
            Long id,
            String title
    ) {
        public static DebateTopicResponse of(com.likelion.realtalk.domain.debate.entity.DebateTopic e) {
            return new DebateTopicResponse(e.getId(), e.getTitle());
        }
    }
}
