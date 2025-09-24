package com.cws.common.exception;

import com.cws.common.error.ErrorCode;

/**
 * 토큰이 유효하지 않을 때 사용되는 예외입니다. 서명 검증 실패, 파싱 오류, 예상 형식 불일치 등 기본적으로 HTTP
 * 401(UNAUTHORIZED)에 매핑됩니다.
 */
public class TokenInvalidException extends BusinessException {
	/**
	 * 기본 메시지 없이 생성(기본 메시지는 ErrorCode 기본값 사용). ErrorCode.TOKEN_INVALID로 고정합니다.
	 */
	public TokenInvalidException() {
		super(ErrorCode.TOKEN_INVALID);
	}

	/**
	 * 사용자 정의 메시지로 생성. 메시지를 null/blank로 주면 ErrorCode의 기본 메시지가 사용됩니다.
	 */
	public TokenInvalidException(String message) {
		super(ErrorCode.TOKEN_INVALID, message);
	}

	/**
	 * 원인 예외 포함 생성자(파싱/검증 라이브러리 예외 래핑용).
	 */
	public TokenInvalidException(String message, Throwable cause) {
		super(ErrorCode.TOKEN_INVALID, message, cause);
	}
}