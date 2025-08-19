package com.likelion.realtalk.domain.debate.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SideStatsDto {
    long countA;
    long countB;
    long total;
    int percentA; // 0~100
    int percentB; // 0~100
}
