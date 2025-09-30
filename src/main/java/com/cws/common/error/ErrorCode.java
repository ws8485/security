package com.cws.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 400 Bad Request
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    INVALID_PARAMETER("INVALID_PARAMETER", HttpStatus.BAD_REQUEST, "요청 파라미터가 올바르지 않습니다."),
    DUPLICATE_REQUEST("DUPLICATE_REQUEST", HttpStatus.BAD_REQUEST, "중복된 요청입니다."),

    // 401 Unauthorized
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_INVALID("TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_REUSED("TOKEN_REUSED", HttpStatus.UNAUTHORIZED, "재사용이 감지된 리프레시 토큰입니다."),

    // 403 Forbidden
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 404 Not Found
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // 409 Conflict
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "리소스 충돌이 발생했습니다."),

    // 429 Too Many Requests
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    // 500 Internal Server Error
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

    // 502/503/504
    BAD_GATEWAY("BAD_GATEWAY", HttpStatus.BAD_GATEWAY, "업스트림 서버 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "서비스를 일시적으로 사용할 수 없습니다."),
    GATEWAY_TIMEOUT("GATEWAY_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT, "업스트림 서버 응답이 지연되고 있습니다.");

    private final String code;          // 추가
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() { return code; }          // 추가
    public HttpStatus getStatus() { return status; }
    public String getDefaultMessage() { return defaultMessage; }
}