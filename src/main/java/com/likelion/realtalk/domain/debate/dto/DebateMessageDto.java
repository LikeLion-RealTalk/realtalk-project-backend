package com.likelion.realtalk.domain.debate.dto;

import com.likelion.realtalk.domain.debate.type.Side;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DebateMessageDto {
  private String roomUUID;
  private Long userId;
  private String message;
  private Side side;

  public DebateMessageDto(String roomUUID, Long userId, String message, Side side) {
    this.roomUUID = roomUUID;
    this.userId = userId;
    this.message = message;
    this.side = side;
  }
}