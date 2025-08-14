package com.likelion.realtalk.domain.debate.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class SpeechToTextService {

//  private static final String KEY_PATH = "keys/gothic-standard-448011-n6-67240501fdde.json";
  private static final String KEY_PATH = System.getenv("GCP_KEY_PATH");

  /**
   * 단발성 STT 처리 (base64 또는 byte[] 오디오 → 텍스트 반환)
   */
  public String recognize(byte[] audioBytes) {
    if (audioBytes == null || audioBytes.length == 0) {
      throw new IllegalArgumentException("오디오 데이터가 비어있습니다.");
    }

//    try (InputStream keyStream = getClass().getClassLoader().getResourceAsStream(KEY_PATH)) {
//      if (keyStream == null) {
//        throw new IllegalStateException("Google Cloud Key 파일을 찾을 수 없습니다: " + KEY_PATH);
//      }
    if (KEY_PATH == null || KEY_PATH.isEmpty()) {
      throw new IllegalStateException("GCP_KEY_PATH 환경 변수가 설정되지 않았습니다.");
    }
    try (InputStream keyStream = Files.newInputStream(Path.of(KEY_PATH))) {

      SpeechSettings speechSettings = SpeechSettings.newBuilder()
          .setCredentialsProvider(FixedCredentialsProvider.create(
              ServiceAccountCredentials.fromStream(keyStream)
          ))
          .build();

      try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
        RecognitionConfig config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
            .setLanguageCode("ko-KR")
            .setSampleRateHertz(48000)
            .build();

        RecognitionAudio audio = RecognitionAudio.newBuilder()
            .setContent(ByteString.copyFrom(audioBytes))
            .build();

        RecognizeResponse response = speechClient.recognize(config, audio);

        for (SpeechRecognitionResult result : response.getResultsList()) {
          if (result.getAlternativesCount() > 0) {
            return result.getAlternatives(0).getTranscript();
          }
        }
      }
    } catch (Exception e) {
      log.error("STT 처리 실패", e);
      throw new RuntimeException("STT 변환 중 오류가 발생했습니다.", e);
    }

    return "";
  }
}
