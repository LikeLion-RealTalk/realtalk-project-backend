package com.likelion.realtalk.domain.debate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AiMessageDto {

  public String summary;
  public Long userId;
  public Long username;

  @Builder
  public AiMessageDto(String summary, Long userId, Long username) {
    this.summary = summary;
    this.userId = userId;
    this.username = username;
  }
}