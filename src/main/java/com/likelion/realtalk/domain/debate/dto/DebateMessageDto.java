package com.likelion.realtalk.domain.debate.dto;

import com.likelion.realtalk.domain.debate.type.Side;
import lombok.Getter;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DebateMessageDto {
  private String roomUUID;
  private Long userId;
  private String message;
  private Side side;

  //얘네는 음성 발언시 추가로 필요한 필드
  private String mode;  // "녹음 시작", "녹음 중지", "종료"

  public DebateMessageDto(String roomUUID, Long userId, String message, Side side, String mode) {
    this.roomUUID = roomUUID;
    this.userId = userId;
    this.message = message;
    this.side = side;
    this.mode = mode;
  }
}