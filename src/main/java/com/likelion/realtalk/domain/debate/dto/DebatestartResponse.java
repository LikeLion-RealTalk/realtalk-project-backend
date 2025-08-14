package com.likelion.realtalk.domain.debate.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 시작버튼을 눌렀을 때 응답을 받기 위한 DTO 입니다.
 *
 * @author : 오승훈
 * @fileName : DebatestartResponse
 * @since : 2025-08-13
 */
@Data
@Builder
public class DebatestartResponse {

  private String status;
  private LocalDateTime startedAt; //시작시간 갱신을 위한 변수 추가

}