package com.likelion.realtalk.domain.debate.dto;

import com.likelion.realtalk.domain.debate.type.Side;
import java.util.ArrayList;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpeakerMessageDto {

  private String message;
  private ArrayList<String> sourceLinks;
  private String verificationResult;
  private String evidence;
  private Long userId;
  private String username;
  private Side side;

  @Builder(toBuilder = true)
  public SpeakerMessageDto(String message, ArrayList<String> sourceLinks,
      String verificationResult,
      String evidence, Long userId, String username, Side side) {
    this.message = message;
    this.sourceLinks = sourceLinks;
    this.verificationResult = verificationResult;
    this.evidence = evidence;
    this.userId = userId;
    this.username = username;
    this.side = side;
  }
}