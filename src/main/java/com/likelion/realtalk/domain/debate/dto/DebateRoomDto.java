package com.likelion.realtalk.domain.debate.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class DebateRoomDto {
  private String roomUUID;
  private Long roomId;
  private List<Long> userIds;
  private String debateType;

  public DebateRoomDto(String roomUUID, Long roomId, List<Long> userIds, String debateType) {
    this.roomUUID = roomUUID;
    this.roomId = roomId;
    this.userIds = userIds;
    this.debateType = debateType;
  }
}