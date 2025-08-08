package com.likelion.realtalk.domain.debate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.dto.AiMessageDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import com.likelion.realtalk.infra.claude.ClaudeAiClient;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

  private final ObjectMapper objectMapper;
  private final ClaudeAiClient claudeAiClient;

  // ai 팩트 체킹 메서드
  public SpeakerMessageDto factcheck(String text) {
    String response = claudeAiClient.call("{\n"
        + "  \"message\": \"You are a debate fact-checking expert. The statement below may be in Korean or English."
        + "\\n\\nStatement: \"" + text + "\\n\\nYour task:"
        + "\\n1. Analyze the statement regardless of language"
        + "\\n2. Determine if this is a FACTUAL claim that can be verified"
        + "\\n3. Search for credible sources to verify the claim"
        + "\\n4. Respond in the SAME LANGUAGE as the input statement\\"
        + "n5. Return response in valid JSON format\\n\\nRequired JSON response format:\\n{\\n  "
        + "\"verificationResult\": \"사실\" | \"거짓\" | \"검증 불가\",\\n  "
        + "\"evidence\": \"\"Provide evidence as a short and concise single sentence\",\\n  "
        + "\"sourceLinks\": [\"url1\", \"url2\", \"url3\"]\\n}\\n"
        + "\\nIMPORTANT: \\n- source_links MUST be an array/list format: [\"url1\", \"url2\"]"
        + "\\n- If no sources found, return empty array: []"
        + "\\n- Do NOT return null or string, only array formaght\"\n" + "}");

    String jsonPart = response.substring(0, response.indexOf("}") + 1);

    return convertToSpeakerMessageDto(jsonPart);
  }

  // ai 요약 메서드
  public AiMessageDto summary(String text) {
    String response = claudeAiClient.call(
        "You are responsible for understanding users' discussion content and concisely summarizing the key points.\n"
            + "\n" + "Below is a statement made by a user during the discussion:\n" + text
            + "When summarizing, be sure to strictly follow these conditions:\n"
            + "Summarize in Korean.\n"
            + "Omit unnecessary expressions and focus on the core argument or opinion, summarizing in 2–3 sentences.\n"
            + "Respond in the following JSON format:\n"
            + "{summary: \"2–3 sentence summary content\"}");

    String jsonPart = response.substring(0, response.indexOf("}") + 1);

    return convertToAiMessageDto(jsonPart);
  }

  private SpeakerMessageDto convertToSpeakerMessageDto(String json) {
    try {
      return objectMapper.readValue(json, SpeakerMessageDto.class);
    } catch (IOException e) {
      throw new RuntimeException("JSON 변환 실패", e);
    }
  }

  private AiMessageDto convertToAiMessageDto(String json) {
    try {
      return objectMapper.readValue(json, AiMessageDto.class);
    } catch (IOException e) {
      throw new RuntimeException("JSON 변환 실패", e);
    }
  }
}