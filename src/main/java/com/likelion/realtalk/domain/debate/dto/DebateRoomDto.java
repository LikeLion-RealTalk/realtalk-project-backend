package com.likelion.realtalk.domain.debate.dto;

import com.likelion.realtalk.domain.debate.type.DebateType;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
public class DebateRoomDto {
  private String roomUUID;
  private List<Long> userIds;
  private DebateType debateType;

  @Builder
  public DebateRoomDto(String roomUUID, List<Long> userIds, DebateType debateType) {
    this.roomUUID = roomUUID;
    this.userIds = userIds;
    this.debateType = debateType;
  }
}