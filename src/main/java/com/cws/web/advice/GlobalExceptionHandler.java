package com.cws.web.advice;

import com.cws.common.error.ErrorCode;
import com.cws.common.error.ErrorResponse;
import com.cws.common.exception.BadRequestException;
import com.cws.common.exception.BusinessException;
import com.cws.common.exception.TokenInvalidException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기. 컨트롤러 레이어에서 발생하는 예외를 일관된 HTTP 상태와 ErrorResponse 스키마로 변환합니다.
 * 인증(401), 인가(403), 검증(400), 미처리(500) 등을 명확히 구분합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
// 필요 시 MDC 또는 헤더(X-Request-Id)에서 추출하도록 확장하세요.
	private String traceId() {
		return null;
	}

	private ResponseEntity build(ErrorCode code, String message, HttpServletRequest request) {
		return ResponseEntity.status(code.getStatus())
				.body(ErrorResponse.of(code, message, traceId(), request.getRequestURI()));
	}

	/**
	 *
	 * Bean Validation(@Valid) 실패 등 바인딩 오류 → 400
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream().findFirst()
				.map(err -> err.getField() + " " + err.getDefaultMessage())
				.orElse(ErrorCode.VALIDATION_FAILED.getDefaultMessage());
		return build(ErrorCode.VALIDATION_FAILED, message, request);
	}

	/**
	 *
	 * @ModelAttribute 바인딩 실패 등 → 400
	 */
	@ExceptionHandler(BindException.class)
	public ResponseEntity handleBindException(BindException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream().findFirst()
				.map(err -> err.getField() + " " + err.getDefaultMessage())
				.orElse(ErrorCode.BAD_REQUEST.getDefaultMessage());
		return build(ErrorCode.BAD_REQUEST, message, request);
	}

	/**
	 *
	 * 필수 파라미터 누락 → 400
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
		String message = String.format("필수 파라미터 누락: %s", ex.getParameterName());
		return build(ErrorCode.INVALID_PARAMETER, message, request);
	}

	/**
	 *
	 * 지원하지 않는 HTTP 메서드 → 400 혹은 405. 여기서는 클라이언트 수정이 필요한 케이스로 400으로 통일합니다.
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		String message = "지원하지 않는 요청 메서드입니다.";
		return build(ErrorCode.BAD_REQUEST, message, request);
	}

	/**
	 *
	 * 자격 증명 오류(아이디/비밀번호 불일치 등) → 401
	 */
	@ExceptionHandler({ BadCredentialsException.class, UsernameNotFoundException.class })
	public ResponseEntity handleAuthFailure(RuntimeException ex, HttpServletRequest request) {
		// 보안상
		// 상세
		// 사유
		// 노출
		// 금지
		return build(ErrorCode.AUTH_INVALID_CREDENTIALS, null, request);

	}

	/**
	 *
	 * 토큰 유효성 문제(서명 불일치, 포맷 오류 등) → 401
	 */
	@ExceptionHandler(TokenInvalidException.class)
	public ResponseEntity handleTokenInvalid(TokenInvalidException ex, HttpServletRequest request) {
		return build(ErrorCode.TOKEN_INVALID, ex.getMessage(), request);
	}

	/**
	 *
	 * 권한 부족 → 403
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return build(ErrorCode.ACCESS_DENIED, null, request);
	}

	/**
	 *
	 * 도메인 비즈니스 규칙 위반 → ErrorCode에 따른 상태값으로 응답
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity handleBusiness(BusinessException ex, HttpServletRequest request) {
		ErrorCode code = ex.getErrorCode();
		return build(code, ex.getMessage(), request);
	}

	/**
	 *
	 * 클라이언트 잘못된 요청(명시적) → 400
	 */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		ErrorCode code = ex.getErrorCode();
		return build(code, ex.getMessage(), request);
	}

	/**
	 * 나머지 처리되지 않은 예외 → 500
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity handleGeneral(Exception ex, HttpServletRequest request) {
		// 내부 로그에서는 ex를 충분히 기록하되, 응답 메시지는 일반화합니다.
		return build(ErrorCode.INTERNAL_ERROR, null, request);
	}
}
