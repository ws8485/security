package com.cws.security.handler;

import com.cws.common.error.ErrorCode;
import com.cws.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *
 * 인가 실패(403)를 JSON 바디로 응답하는 핸들러. 스프링 시큐리티 필터 단계에서 권한 부족이 발생했을 때 동작합니다.
 * ErrorResponse 스키마로 일관된 응답을 제공합니다.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public RestAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {

		ErrorCode code = ErrorCode.ACCESS_DENIED;

		response.setStatus(code.getStatus().value()); // 403
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// traceId는 필요 시 MDC나 헤더(X-Request-Id)에서 가져오도록 확장하세요.
		String traceId = null;
		String path = request.getRequestURI();

		ErrorResponse body = ErrorResponse.of(code, null, traceId, path);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}