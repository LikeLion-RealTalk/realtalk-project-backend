package com.likelion.realtalk.domain.debate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.realtalk.domain.debate.dto.AiSummaryDto;
import com.likelion.realtalk.domain.debate.dto.DebateResultDto.AiSummaryResultDto;
import com.likelion.realtalk.domain.debate.dto.SpeakerMessageDto;
import com.likelion.realtalk.domain.debate.repository.DebateRedisRepository;
import com.likelion.realtalk.global.exception.DataRetrievalException;
import com.likelion.realtalk.global.exception.ErrorCode;
import com.likelion.realtalk.global.redis.RedisKeyUtil;
import com.likelion.realtalk.infra.claude.ClaudeAiClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

  private final ObjectMapper objectMapper;
  private final ClaudeAiClient claudeAiClient;
  private final DebateRedisRepository debateRedisRepository;

  // 토론방 최초 입장 시 AI 요약 내용들을 조회하는 메서드
  public ArrayList<AiSummaryDto> getAiSummaries(String roomUUID) {
    String turnValue = debateRedisRepository.getRoomField(roomUUID, "turn");
    if (turnValue == null) {
      return new ArrayList<>(); // 또는 예외 던짐
    }
    int turn = Integer.parseInt(turnValue);

    ArrayList<AiSummaryDto> aiSummaryDtoDtos = new ArrayList<>();
    String aiSummariesKey = RedisKeyUtil.getAiSummariesKey(roomUUID);
    for (int i = 1; i <= turn; i++) {

      List<AiSummaryDto> aiSummaries = debateRedisRepository.getAiSummaries(aiSummariesKey,
          String.valueOf(i));

      if (aiSummaries != null && !aiSummaries.isEmpty()) {
        aiSummaryDtoDtos.addAll(aiSummaries);
      }
    }
    return aiSummaryDtoDtos;
  }

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
  public AiSummaryDto summary(String text) {
    String response = claudeAiClient.call(
        "You are responsible for understanding users' discussion content and concisely summarizing the key points.\n"
            + "\n" + "Below is a statement made by a user during the discussion:\n" + text
            + "When summarizing, be sure to strictly follow these conditions:\n"
            + "Summarize in Korean.\n"
            + "Omit unnecessary expressions and focus on the core argument or opinion, summarizing in 2–3 sentences.\n"
            + "Respond in the following JSON format:\n"
            + "{summary: \"2–3 sentence summary content\"}");

    String jsonPart = response.substring(0, response.indexOf("}") + 1);

    return convertToAiSummaryDto(jsonPart);
  }

  // ai 전체 요약 메서드
  public AiSummaryResultDto summaryResult(ArrayList<SpeakerMessageDto> speeches) throws IOException {

    String response = claudeAiClient.call(
        "You are responsible for understanding the discussion between users and summarizing the key points concisely.\n"
            + "Below is the conversation between the sideA side and the sideB side:\n" + "\n"
            + convertSpeechesToAiInput(speeches) + "\n" + "\n"
            + "When summarizing, you must follow these rules:\n" + "\n" + "Summarize in Korean.\n"
            + "\n"
            + "Omit unnecessary expressions and focus on summarizing the main arguments or opinions from the statements.\\\n"
            + "\n" + "Respond in the following JSON format: \"{\n"
            + "  \"sideA\": \"1 sentence summary of sideA content\",\n"
            + "  \"sideB\": \"1 sentence summary of sideB content\",\n"
            + "  \"aiResult\": \"2-3 sentence summary of all content\"\n" + "}\"");

    String jsonPart = response.substring(0, response.indexOf("}") + 1);

    return convertToAiSummaryResultDto(jsonPart);
  }

  private String convertSpeechesToAiInput(List<SpeakerMessageDto> speeches) {
    StringBuilder sb = new StringBuilder();

    for (SpeakerMessageDto speech : speeches) {
      sb.append(speech.getSide().toString()).append(": ").append(speech.getMessage())
          .append("\n");
    }

    return sb.toString().trim(); // 마지막 줄바꿈 제거
  }

  private SpeakerMessageDto convertToSpeakerMessageDto(String json) {
    try {
      return objectMapper.readValue(json, SpeakerMessageDto.class);
    } catch (IOException e) {
      log.error("ai 팩트 채킹 response 오류 : {}", json);
      return SpeakerMessageDto.failure();
    }
  }

  private AiSummaryDto convertToAiSummaryDto(String json) {
    try {
      return objectMapper.readValue(json, AiSummaryDto.class);
    } catch (IOException e) {
      log.error("ai 요약 response 오류 {}", json);
      return AiSummaryDto.failure();
    }
  }

  private AiSummaryResultDto convertToAiSummaryResultDto(String json) throws IOException {
      return objectMapper.readValue(json, AiSummaryResultDto.class);
  }

  // redis 정보 삭제
  public void clearAiCaches(String roomUUID) {
    this.debateRedisRepository.deleteByKey(RedisKeyUtil.getAiSummariesKey(roomUUID));
  }

}