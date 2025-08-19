package com.likelion.realtalk.domain.debate.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class ChangeSideRequest {
  private UUID roomId;     // 방 UUID
  private String subjectId; // 유저 식별자(게스트: guest:xxxx 형태 등)
  private String side;     // "A" or "B"
}
