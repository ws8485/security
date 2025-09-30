package com.cws.common.exception;

import com.cws.common.error.ErrorCode;

/**
 * 토큰이 만료되었을 때 사용하는 예외입니다. 기본적으로 401(UNAUTHORIZED)에 매핑됩니다.
 */
public class TokenExpiredException extends BusinessException {

    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(ErrorCode.TOKEN_EXPIRED, message, cause);
    }
}