package com.cws.web.advice;

import com.cws.common.error.ErrorCode;
import com.cws.common.error.ErrorResponse;
import com.cws.common.exception.BusinessException;
import com.cws.common.exception.TokenExpiredException;
import com.cws.common.exception.TokenInvalidException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리기. 컨트롤러 레이어에서 발생하는 예외를 일관된 HTTP 상태와 ErrorResponse 스키마로 변환합니다.
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 토큰 만료 → 401
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException ex, HttpServletRequest request) {
        ErrorCode code = ErrorCode.TOKEN_EXPIRED;
        return build(code, ex.getMessage(), request.getRequestURI(), resolveTraceId(request));
    }

    // 토큰 무효 → 401
    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalid(TokenInvalidException ex, HttpServletRequest request) {
        ErrorCode code = ErrorCode.TOKEN_INVALID;
        return build(code, ex.getMessage(), request.getRequestURI(), resolveTraceId(request));
    }

    // 접근 거부 → 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorCode code = ErrorCode.ACCESS_DENIED;
        return build(code, ex.getMessage(), request.getRequestURI(), resolveTraceId(request));
    }

    // 요청 검증 실패(@Valid) → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(code, message, request.getRequestURI(), resolveTraceId(request));
    }

    // 바인딩 오류(쿼리/폼 바인딩 실패) → 400
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(code, message, request.getRequestURI(), resolveTraceId(request));
    }

    // 필수 파라미터 누락 → 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        ErrorCode code = ErrorCode.INVALID_PARAMETER;
        String message = ex.getParameterName() + " 파라미터가 필요합니다.";
        return build(code, message, request.getRequestURI(), resolveTraceId(request));
    }

    // 메서드/미디어 타입 미지원
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        ErrorCode code = ErrorCode.BAD_REQUEST;
        String message = "지원하지 않는 메서드입니다. (" + ex.getMethod() + ")";
        return buildWithStatus(HttpStatus.METHOD_NOT_ALLOWED, code, message, request.getRequestURI(), resolveTraceId(request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpServletRequest request) {
        ErrorCode code = ErrorCode.BAD_REQUEST;
        String message = "지원하지 않는 미디어 타입입니다.";
        return buildWithStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, code, message, request.getRequestURI(), resolveTraceId(request));
    }

    // 도메인/비즈니스 예외 → 각 ErrorCode의 상태로 응답
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : code.getDefaultMessage();
        return build(code, message, request.getRequestURI(), resolveTraceId(request));
    }

    // 마지막 방어막 → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // log.error("Unexpected error", ex);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return build(code, code.getDefaultMessage(), request.getRequestURI(), resolveTraceId(request));
    }

    // 공통 빌더 
    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, String path, String traceId) {
        HttpStatus status = code.getStatus();
        String finalMessage = (message == null || message.isBlank()) ? code.getDefaultMessage() : message;
        ErrorResponse body = ErrorResponse.of(code, finalMessage, traceId, path);
        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    private ResponseEntity<ErrorResponse> buildWithStatus(HttpStatus status, ErrorCode code, String message, String path, String traceId) {
        String finalMessage = (message == null || message.isBlank()) ? code.getDefaultMessage() : message;
        ErrorResponse body = ErrorResponse.of(code, finalMessage, traceId, path);
        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    // traceId 추출: 우선순위 헤더 → MDC
    private String resolveTraceId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        if (id != null && !id.isBlank()) return id;
        String mdcId = MDC.get("traceId");
        return (mdcId != null) ? mdcId : "";
    }
}