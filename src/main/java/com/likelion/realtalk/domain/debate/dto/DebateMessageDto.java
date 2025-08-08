package com.likelion.realtalk.domain.debate.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DebateMessageDto {
  private String roomUUID;
  private Long userId;
  private String message;

  public DebateMessageDto(String roomUUID, Long userId, String message) {
    this.roomUUID = roomUUID;
    this.userId = userId;
    this.message = message;
  }
}