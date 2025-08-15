package com.likelion.realtalk.domain.debate.dto;

import com.likelion.realtalk.domain.debate.type.DebateType;
import com.likelion.realtalk.domain.debate.entity.DebateRoom;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DebateRoomResponse {

    private UUID roomId;
    private String title;
    private String status;

    private CategoryDto category;

    private String sideA;
    private String sideB;

    private Long maxSpeaker;
    private Long maxAudience;

    private DebateType debateType;

    private Long currentSpeaker;
    private Long currentAudience;

    private Long elapsedSeconds; // 현재까지 진행된 초

    /** 엔티티 -> DTO 변환 (간단 버전) */
    public static DebateRoomResponse from(com.likelion.realtalk.domain.debate.entity.DebateRoom room,
                                        java.util.UUID roomUuid) {

        Long categoryId = null;
        String categoryName = null;

        if (room.getCategory() != null) {
            categoryId = room.getCategory().getId();     // ★ 연관 엔티티의 PK
            // 필요 시만 사용하세요. LAZY면 추가 쿼리 발생 가능
            // categoryName = room.getCategory().getName();
        }

        return DebateRoomResponse.builder()
                .roomId(roomUuid) // ★ Redis 매핑으로 받은 UUID
                .title(room.getTitle())
                .status(room.getStatus().name())
                .category(CategoryDto.builder()
                        .id(categoryId)
                        .name(categoryName) // 이름이 필요 없으면 null 유지
                        .build())
                .sideA(room.getSideA())
                .sideB(room.getSideB())
                .maxSpeaker(room.getMaxSpeaker())
                .maxAudience(room.getMaxAudience())
                .debateType(room.getDebateType())
                .currentSpeaker(0L)
                .currentAudience(0L)
                .elapsedSeconds(0L)
                .build();
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryDto {
        private Long id;
        private String name;
    }
}
