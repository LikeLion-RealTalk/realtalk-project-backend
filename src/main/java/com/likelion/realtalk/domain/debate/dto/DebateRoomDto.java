package com.likelion.realtalk.domain.debate.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class DebateRoomDto {
  private String roomUUID;
  private List<Long> userIds;
  private String debateType;

  public DebateRoomDto(String roomUUID, List<Long> userIds, String debateType) {
    this.roomUUID = roomUUID;
    this.userIds = userIds;
    this.debateType = debateType;
  }
}