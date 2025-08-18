package com.likelion.realtalk.global.exception;


import com.likelion.realtalk.global.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** 1) 잘못된 요청 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("잘못된 요청: {}", ex.getMessage());
        return ResponseEntity.badRequest().body("잘못된 요청입니다: " + ex.getMessage());
    }

    @ExceptionHandler(DataRetrievalException.class)
    public ResponseEntity<ErrorResponse> handleDataRetrievalException(DataRetrievalException e) {
        log.error("데이터 조회 오류: {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(
            ErrorResponse.of(e.getErrorCode())
        );
    }

    @ExceptionHandler(DebateRoomValidationException.class)
    public ResponseEntity<ErrorResponse> handleDebateRoomValidationException(DebateRoomValidationException e) {
        log.error("토론방 유효성 오류: {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(
            ErrorResponse.of(e.getErrorCode())
        );
    }

    /** 2) 예상치 못한 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("서버 내부 오류 발생: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 오류가 발생했습니다.");
    }

    /** 3) @Valid 검증 실패 (Spring 6 시그니처) */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("유효성 검증 실패: {}", errorMessage);
        return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다: " + errorMessage);
    }

    // 필요 시 커스텀 예외 핸들러 추가 (예: RoomNotFoundException)
    // @ExceptionHandler(RoomNotFoundException.class)
    // public ResponseEntity<String> handleRoomNotFoundException(RoomNotFoundException ex) {
    //     log.warn("토론방을 찾을 수 없습니다: {}", ex.getMessage());
    //     return ResponseEntity.status(HttpStatus.NOT_FOUND).body("토론방이 존재하지 않습니다.");
    // }
}
