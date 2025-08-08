package com.likelion.realtalk.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClaudeAiConfig {

  @Bean
  public ChatClient chatClient(ChatModel chatModel) {
    return ChatClient.create(chatModel);
  }

}