package com.cws.common.exception;

import com.cws.common.error.ErrorCode;

/**
 * 비즈니스 규칙 위반을 표현하는 공통 런타임 예외입니다. ErrorCode를 포함하여 전역 예외 처리기에서 일관된 HTTP 상태/메시지로
 * 매핑할 수 있습니다. 도메인별 세부 예외는 본 클래스를 상속하거나, 이 클래스를 직접 사용해도 됩니다.
 */
public class BusinessException extends RuntimeException {
	private final ErrorCode errorCode;

	/**
	 * 필수 생성자. ErrorCode의 기본 메시지를 사용하려면 message를 null 또는 빈 문자열로 전달하세요.
	 */
	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = (errorCode != null) ? errorCode : ErrorCode.INTERNAL_ERROR;
	}

	/**
	 *
	 * 원인 예외 포함 생성자.
	 */
	public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = (errorCode != null) ? errorCode : ErrorCode.INTERNAL_ERROR;
	}

	/**
	 *
	 * 기본 메시지(override 없음)로 생성.
	 */
	public BusinessException(ErrorCode errorCode) {
		super();
		this.errorCode = (errorCode != null) ? errorCode : ErrorCode.INTERNAL_ERROR;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}