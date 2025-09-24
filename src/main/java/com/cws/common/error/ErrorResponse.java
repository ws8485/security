package com.cws.common.error;

import java.time.Instant;
import java.util.Objects;

/**
 * 표준 에러 응답 DTO. 응답 스키마를 통일하여 클라이언트 처리 로직을 단순화합니다. code는 ErrorCode의 name()을 그대로
 * 사용합니다.
 */
public final class ErrorResponse {
	private final String code; // 에러 코드 식별자 (예: AUTH_INVALID_CREDENTIALS)
	private final String message; // 사용자 노출용 메시지(민감 정보 금지)
	private final String traceId; // 요청 추적용 ID(MDC나 X-Request-Id 등)
	private final Instant timestamp; // UNIX epoch millis
	private final String path; // 요청 경로

	/**
	 * ErrorCode의 기본 메시지를 사용하여 응답을 생성합니다.
	 */
	public static ErrorResponse of(ErrorCode errorCode, String traceId, String path) {
		Objects.requireNonNull(errorCode, "errorCode must not be null");
		return new ErrorResponse(errorCode.name(), errorCode.getDefaultMessage(), traceId, Instant.now(),
				path);
	}

	/**
	 * 커스텀 메시지로 기본 메시지를 대체하여 응답을 생성합니다. overrideMessage가 null/blank이면 기본 메시지를 사용합니다.
	 */
	public static ErrorResponse of(ErrorCode errorCode, String overrideMessage, String traceId, String path) {
		Objects.requireNonNull(errorCode, "errorCode must not be null");
		String msg = (overrideMessage == null || overrideMessage.isBlank()) ? errorCode.getDefaultMessage()
				: overrideMessage;
		return new ErrorResponse(errorCode.name(), msg, traceId, Instant.now(), path);
	}

	private ErrorResponse(String code, String message, String traceId, Instant timestamp, String path) {
		this.code = code;
		this.message = message;
		this.traceId = traceId;
		this.timestamp = timestamp;
		this.path = path;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public String getTraceId() {
		return traceId;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		return "ErrorResponse [code=" + code + ", message=" + message + ", traceId=" + traceId + ", timestamp="
				+ timestamp + ", path=" + path + "]";
	}

}