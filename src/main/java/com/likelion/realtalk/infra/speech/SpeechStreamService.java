package com.likelion.realtalk.infra.speech;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.function.Consumer;

@Slf4j
@Service
public class SpeechStreamService {

    private static final String KEY_PATH = "keys/gothic-standard-448011-n6-67240501fdde.json";

    /**
     * WebSocket에서 받은 오디오를 STT로 변환하는 스트리밍 세션 생성
     * @param onTranscript 인식된 텍스트를 처리하는 콜백
     * @return ClientStream (이 스트림에 오디오 데이터 전송)
     */
    public ClientStream<StreamingRecognizeRequest> createStream(Consumer<String> onTranscript) throws Exception {

        // Google 인증 키 로드
        InputStream keyStream = getClass().getClassLoader().getResourceAsStream(KEY_PATH);
        if (keyStream == null) {
            throw new IllegalStateException("Google Cloud Key 파일을 찾을 수 없습니다: " + KEY_PATH);
        }

        SpeechSettings speechSettings = SpeechSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(
                ServiceAccountCredentials.fromStream(keyStream)
            ))
            .build();

        SpeechClient client = SpeechClient.create(speechSettings);

        // 오디오 인식 설정 (클라이언트와 동일한 포맷)
        RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS) // 브라우저 전송 포맷 맞춤
            .setLanguageCode("ko-KR")
            .setSampleRateHertz(48000)
            .build();

        StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(recognitionConfig)
            .setInterimResults(true)
            .build();

        // 응답 처리
        ResponseObserver<StreamingRecognizeResponse> responseObserver = new ResponseObserver<>() {
            @Override
            public void onStart(StreamController controller) {}

            @Override
            public void onResponse(StreamingRecognizeResponse response) {
                for (StreamingRecognitionResult result : response.getResultsList()) {
                    if (result.getAlternativesCount() > 0) {
                        String transcript = result.getAlternatives(0).getTranscript();
                        if (!transcript.isBlank()) {
                            onTranscript.accept(transcript);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("STT 오류", t);
            }

            @Override
            public void onComplete() {
                log.info("STT 스트리밍 완료");
            }
        };

        // STT 스트림 생성
        ClientStream<StreamingRecognizeRequest> stream = client.streamingRecognizeCallable().splitCall(responseObserver);

        // 첫 설정 패킷 전송
        stream.send(StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(streamingRecognitionConfig)
            .build());

        return stream;
    }

    /**
     * WebSocket에서 받은 오디오 데이터를 STT 스트림에 전송
     */
    public void sendAudio(ClientStream<StreamingRecognizeRequest> stream, byte[] audioBytes) {
        if (stream != null && audioBytes != null && audioBytes.length > 0) {
            stream.send(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(audioBytes))
                .build());
        }
    }
}
