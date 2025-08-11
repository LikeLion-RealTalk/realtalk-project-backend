package com.likelion.realtalk.infra.claude;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClaudeAiClient {

  private final ChatClient chatClient;

  public String call(String prompt) {
    return chatClient.prompt()
        .user(prompt)
        .call()
        .content();
  }
}