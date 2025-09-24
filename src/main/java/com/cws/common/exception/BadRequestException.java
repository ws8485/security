package com.cws.common.exception;

import com.cws.common.error.ErrorCode;

public class BadRequestException extends RuntimeException {
	private final ErrorCode errorCode;

	/**
	 * 클라이언트의 잘못된 요청(400)을 표현하는 예외입니다. 기본적으로 HTTP 400에 매핑됩니다. ErrorCode를 함께 전달하면 전역
	 * 예외 처리기에서 일관된 응답 바디를 만들기 쉽습니다.
	 */
	public BadRequestException(String message) {
		super(message);
		this.errorCode = ErrorCode.BAD_REQUEST;
	}

	/**
	 * 기본 메시지로 생성(에러 코드는 BAD_REQUEST로 고정).
	 */
	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = ErrorCode.BAD_REQUEST;
	}

	/**
	 * 원인 예외 포함 생성자.
	 */
	public BadRequestException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode != null ? errorCode : ErrorCode.BAD_REQUEST;
	}

	/**
	 * 특정 ErrorCode와 함께 생성. 메시지를 null/blank로 주면 ErrorCode의 기본 메시지를 사용하세요.
	 */
	public BadRequestException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode != null ? errorCode : ErrorCode.BAD_REQUEST;
	}

	/**
	 * ErrorCode와 원인 예외 포함 생성자.
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
