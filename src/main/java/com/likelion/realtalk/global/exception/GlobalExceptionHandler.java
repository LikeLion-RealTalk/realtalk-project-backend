package com.likelion.realtalk.global.exception;


// import java.net.http.HttpHeaders;
// import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
// import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 1. IllegalArgumentException 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("잘못된 요청: {}", ex.getMessage());
        return ResponseEntity.badRequest().body("잘못된 요청입니다: " + ex.getMessage());
    }

    // 2. 커스텀 예외 처리 (예: RoomNotFoundException)
    // @ExceptionHandler(RoomNotFoundException.class)
    // public ResponseEntity<String> handleRoomNotFoundException(RoomNotFoundException ex) {
    //     log.warn("토론방을 찾을 수 없습니다: {}", ex.getMessage());
    //     return ResponseEntity.status(HttpStatus.NOT_FOUND).body("토론방이 존재하지 않습니다.");
    // }

    // 3. 모든 예외 처리 (예상치 못한 오류)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("서버 내부 오류 발생: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
    }

    // 4. Validation 예외 처리 (BindingResult 없이 Controller에서 @Valid 썼을 때)
    // @Override
    // protected ResponseEntity<Object> handleMethodArgumentNotValid(
    //     MethodArgumentNotValidException ex,
    //     HttpHeaders headers,
    //     HttpStatus status,
    //     WebRequest request) {
    //
    //     String errorMessage = ex.getBindingResult()
    //         .getFieldErrors()
    //         .stream()
    //         .map(error -> error.getField() + ": " + error.getDefaultMessage())
    //         .collect(Collectors.joining(", "));
    //
    //     log.warn("유효성 검증 실패: {}", errorMessage);
    //
    //     return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다: " + errorMessage);
    // }
}
