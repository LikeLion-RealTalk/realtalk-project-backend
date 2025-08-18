package com.likelion.realtalk.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 400 Bad Request
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청이 올바르지 않습니다."),
    //    DUPLICATED_EMAIL(HttpStatus.BAD_REQUEST, "DUPLICATED_EMAIL", "이미 등록된 이메일입니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_FIELD", "필수 입력값이 누락되었습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 유효하지 않습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 데이터가 유효하지 않습니다."),
    DUPLICATED_USER(HttpStatus.BAD_REQUEST, "DUPLICATED_USER", "이미 등록된 사용자입니다."),
    DUPLICATED_NICKNAME(HttpStatus.BAD_REQUEST, "DUPLICATED_NICKNAME", "이미 등록된 닉네임입니다."),

    // 400 Bad Request 파일 관련 에러코드
    UNSUPPORTED_FILE_FORMAT(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_FORMAT", "지원하지 않는 파일 형식입니다."),
    IMAGE_FILE_ONLY(HttpStatus.BAD_REQUEST, "IMAGE_FILE_ONLY", "이미지 파일만 업로드 가능합니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "INVALID_FILE_EXTENSION", "허용되지 않는 파일 확장자입니다."),

    // 400 Bad Request 사용자 아이디 관련 에러코드
    USER_ID_EMPTY(HttpStatus.BAD_REQUEST, "USER_ID_EMPTY", "아이디는 비어있을 수 없습니다."),
    USER_ID_DUPLICATE(HttpStatus.BAD_REQUEST, "USER_ID_DUPLICATE", "이미 사용중인 아이디입니다."),
    USER_ID_LENGTH_INVALID(HttpStatus.BAD_REQUEST, "USER_ID_LENGTH_INVALID", "아이디는 4자 이상 12자 이하로 입력해주세요."),
    USER_ID_UPPERCASE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_ID_UPPERCASE_NOT_ALLOWED", "영문 대문자는 사용할 수 없습니다."),
    USER_ID_SPACE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_ID_SPACE_NOT_ALLOWED", "공백은 포함될 수 없습니다."),
    USER_ID_SPECIAL_CHAR_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_ID_SPECIAL_CHAR_NOT_ALLOWED", "특수문자는 포함될 수 없습니다."),
    USER_ID_EMOJI_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_ID_EMOJI_NOT_ALLOWED", "이모티콘은 사용할 수 없습니다."),
    USER_ID_KOREAN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_ID_KOREAN_NOT_ALLOWED", "한글은 포함될 수 없습니다."),

    // 400 Bad Request 사용자 비밀번호 관련 에러코드
    PASSWORD_LENGTH_INVALID(HttpStatus.BAD_REQUEST, "PASSWORD_LENGTH_INVALID", "패스워드는 6자 이상 18자 이하로 입력해주세요."),
    PASSWORD_NO_LOWERCASE(HttpStatus.BAD_REQUEST, "PASSWORD_NO_LOWERCASE", "영문 소문자가 최소 1자 이상 포함되어야 합니다."),
    PASSWORD_NO_UPPERCASE(HttpStatus.BAD_REQUEST, "PASSWORD_NO_UPPERCASE", "영문 대문자가 최소 1자 이상 포함되어야 합니다."),
    PASSWORD_NO_SPECIAL_CHAR(HttpStatus.BAD_REQUEST, "PASSWORD_NO_SPECIAL_CHAR", "특수문자가 최소 1자 이상 포함되어야 합니다."),
    PASSWORD_SPACE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PASSWORD_SPACE_NOT_ALLOWED", "공백은 포함될 수 없습니다."),
    PASSWORD_EMOJI_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PASSWORD_EMOJI_NOT_ALLOWED", "이모티콘은 사용할 수 없습니다."),
    PASSWORD_KOREAN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PASSWORD_KOREAN_NOT_ALLOWED", "한글은 포함될 수 없습니다."),
    INVALID_DEBATE_STATE(HttpStatus.BAD_REQUEST, "INVALID_DEBATE_STATE", "현재 진행 중인 토론이 아닙니다."),

    // 401 Unauthorized
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "유효하지 않은 토큰입니다."),

    // 403 Forbidden
    SELF_ACCESS_ONLY(HttpStatus.FORBIDDEN, "SELF_ACCESS_ONLY", "본인의 정보만 조회할 수 있습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "이 기능에 접근할 수 없습니다."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 자원이 존재하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다."),
    USER_WITHDRAWN(HttpStatus.NOT_FOUND, "USER_WITHDRAWN", "탈퇴한 회원입니다."),
    DEBATE_NOT_FOUND(HttpStatus.NOT_FOUND, "DEBATE_NOT_FOUND", "해당 토론의 정보를 찾을 수 없습니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "허용되지 않은 HTTP 메서드입니다."),

    // 409 Conflict
    ROOM_ALREADY_STARTED(HttpStatus.CONFLICT, "ROOM_ALREADY_STARTED", "이미 시작되었거나 종료된 방입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "UNKNOWN_ERROR", "예기치 못한 오류가 발생했습니다."),
    JSON_PROCESSING_ERROR(HttpStatus.BAD_REQUEST, "JSON_PROCESSING_ERROR", "JSON 처리 실패."),

    // 500 Internal Server Error - 데이터 조회 실패
    DATA_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "DATA_RETRIEVAL_FAILED", "데이터 조회 중 오류가 발생했습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
