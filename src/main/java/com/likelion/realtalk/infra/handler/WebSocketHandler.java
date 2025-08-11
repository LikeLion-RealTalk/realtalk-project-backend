package com.likelion.realtalk.infra.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
//import com.likelion.realtalk.infra.tts.TextToSpeechService;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketHandler extends BinaryWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<WebSocketSession, SessionContext> sessionMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final TextToSpeechService textToSpeechService;
    private final String keyPath = "keys/gothic-standard-448011-n6-67240501fdde.json";

//    public WebSocketHandler(TextToSpeechService textToSpeechService) {
//        this.textToSpeechService = textToSpeechService;
//    }

    // 세션별 상태
    private static class SessionContext {
        String senderId;
        String mode; // "voice" or "text"
        ByteArrayOutputStream audioBuffer; // 오디오 데이터 누적 버퍼
    }
    
    //웹소켓 연결 성공시 호출되는 메서드
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);

        //실제 웹소켓 아이디
        session.getId();
        SessionContext context = new SessionContext();
        context.senderId = extractSenderId(session);
        sessionMap.put(session, context);
        log.info("WebSocket 연결됨: {}, senderId={}", session.getId(), context.senderId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            SessionContext context = sessionMap.get(session);

            // 1. 모드 설정
            if (json.has("mode")) {
                String mode = json.get("mode").asText();
                if ("speech".equalsIgnoreCase(mode)) mode = "voice";
                context.mode = mode;

                if ("voice".equals(mode)) {
                    initSttSession(context);
                }

                log.info("모드 설정: {} -> {}", context.senderId, mode);
                return;
            }

            // 2. 음성 입력 종료 신호 수신 시 약간 대기 후 STT 실행
            if ("voice".equals(context.mode) && json.has("end") && json.get("end").asBoolean()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // 데이터 수신 완료 대기
                    } catch (InterruptedException ignored) {}
                    processStt(session, context);
                }).start();
                return;
            }

            // 3. 텍스트 발언 (텍스트만 전달 text 변수를 ai 쪽 세정님께 message 전달 할 때 전달해주면 될거 같다)
            if ("text".equals(context.mode) && json.has("content")) {
                String text = json.get("content").asText();
                if (text != null && !text.isEmpty()) {
                    log.info("텍스트 발언: {}", text);

                    // 텍스트 전송 (TTS 안 함)
                    broadcastText(String.format(
                        "{\"type\":\"TEXT\",\"senderId\":\"%s\",\"text\":\"%s\"}",
                        context.senderId, escape(text)
                    ));

                    log.info("텍스트 발언 처리 완료: {}", text);
                }
            }


//            // 3. 텍스트 발언 (TTS 변환)
//            if ("text".equals(context.mode) && json.has("content")) {
//                String text = json.get("content").asText();
//                if (text != null && !text.isEmpty()) {
//                    log.info("TTS 시작: {}", text);
//                    byte[] audio = textToSpeechService.synthesize(text);
//
//                    // 텍스트 전송
//                    broadcastText(String.format(
//                        "{\"type\":\"tts\",\"senderId\":\"%s\",\"text\":\"%s\"}",
//                        context.senderId, escape(text)
//                    ));
//
//                    //Redis 저장 시점
//
//                    /**
//                     * {
//                     * 			"userId": 1,
//                     * 			"message": "저의 의견은 그렇습니다.",
//                     * 			"sourceUrls": [
//                     * 				"www.abc.com",
//                     * 				"www.abc.com"
//                     * 			],
//                     * 			"factChecked": true
//                     * 		    }
//                     */
//
//
//                    // 음성 전송
//                   // broadcastBinary(audio);
//                    broadcastBinaryWithSender(context.senderId, audio);
//
//                    log.info("TTS 발언 처리: {}", text);
//                }
//            }

        } catch (Exception e) {
            log.error("Text 메시지 처리 오류", e);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        SessionContext context = sessionMap.get(session);
        if (context == null || !"voice".equals(context.mode)) return;

        try {
            byte[] audio = message.getPayload().array();
            if (context.audioBuffer != null) {
                context.audioBuffer.write(audio);
                log.info("오디오 데이터 누적: {} bytes", context.audioBuffer.size());
            }
        } catch (Exception e) {
            log.error("오디오 데이터 버퍼링 오류", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        safeClose(session);
        log.info("WebSocket 연결 종료: {}", session.getId());
    }

    // ------------------- STT 초기화 -------------------
    private void initSttSession(SessionContext context) {
        context.audioBuffer = new ByteArrayOutputStream();
    }

    // ------------------- 단발성 Recognize STT 처리 -------------------
    private void processStt(WebSocketSession session, SessionContext context) {
        try {
            byte[] audioBytes = context.audioBuffer != null ? context.audioBuffer.toByteArray() : new byte[0];
            if (audioBytes.length == 0) {
                log.warn("오디오 데이터 없음, STT 요청 건너뜀");
                return;
            }

            try (InputStream keyStream = getClass().getClassLoader().getResourceAsStream(keyPath)) {
                if (keyStream == null) throw new IOException("Google Cloud Key 없음: " + keyPath);

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
                            //tss 변환 결과 텍스트(transcrpit 변수를 ai 쪽 세정님께 message 전달 할 때 전달해주면 될거 같다)
                            String transcript = result.getAlternatives(0).getTranscript();
                            log.info("STT 결과: {}", transcript);

                            // 텍스트 브로드캐스트(wss)
                            broadcastText(String.format(
                                "{\"type\":\"stt\",\"senderId\":\"%s\",\"text\":\"%s\"}", //여기를 아래 json 형식으로 바꾸기
                                context.senderId, escape(transcript)
                            ));


//                            {
//                                "message": "정부 정책에 대한 의견입니다.",
//                                "sourceUrls": ["https://example.com/news"],
//                                "factChecked": true,
//                                "userId": "user-abc",
//                                "username": "홍길동"
//                            }

                            // TTS 변환 후 전송
//                            byte[] ttsAudio = textToSpeechService.synthesize(transcript);
                           // broadcastBinary(ttsAudio);
//                            broadcastBinaryWithSender(context.senderId, ttsAudio);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("STT 요청 실패", e);
        } finally {
            context.audioBuffer = null; // 버퍼 초기화
        }
    }

    private void safeClose(WebSocketSession session) {
        sessions.remove(session);
        sessionMap.remove(session);
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.warn("세션 종료 중 오류", e);
            }
        }
    }

    //프론트에서 생성한 세션id 추출
    private String extractSenderId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null && query.startsWith("senderId=")) {
                return query.substring("senderId=".length());
            }
        } catch (Exception e) {
            log.warn("senderId 추출 실패", e);
        }
        return "anonymous";
    }


    private void broadcastText(String json) {
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {

                    s.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("텍스트 전송 실패", e);
                }
            }
        }
    }
    
    // 나중에 세정님에게 받은 response wss로 보내줄때 수정해야할 코드
//    //message
//    private void broadcastText(String json) {
//        for (WebSocketSession s : sessions) {
//            if (s.isOpen()) {
//                try {
//
//                    s.sendMessage(new TextMessage(aiService(json)));
//                } catch (IOException e) {
//                    log.error("텍스트 전송 실패", e);
//                }
//            }
//        }
//    }
//
//    public String aiService(String text) {
//        return "ai가 분석한 메세지";
//    }

    private void broadcastBinary(byte[] audio) {
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new BinaryMessage(audio));
                } catch (IOException e) {
                    log.error("음성 전송 실패", e);
                }
            }
        }
    }


    //웹소켓 세션 아이디(senderId)
    private void broadcastBinaryWithSender(String senderId, byte[] audio) {
        String base64Audio = Base64.getEncoder().encodeToString(audio);
        String json = String.format(
            "{\"type\":\"audio\",\"senderId\":\"%s\",\"audio\":\"%s\"}",
            senderId, base64Audio
        );

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("오디오 전송 실패", e);
                }
            }
        }
    }



    private String escape(String text) {
        return text.replace("\"", "\\\"").replace("\n", " ");
    }
}
