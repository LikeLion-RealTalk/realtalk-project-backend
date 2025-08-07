package com.likelion.realtalk.domain.debate.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DebateMessageDto {
  private Long userId;
  private String message;

  public DebateMessageDto(Long userId, String message) {
    this.userId = userId;
    this.message = message;
  }
}