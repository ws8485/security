package com.cws.security.handler;

import com.cws.common.error.ErrorCode;
import com.cws.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패/미인증(401)을 JSON 바디로 응답하는 엔트리 포인트. 스프링 시큐리티 필터 단계에서 인증이 없거나 실패했을 때 동작합니다.
 * ErrorResponse 스키마로 일관된 응답을 제공합니다.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ErrorCode code = ErrorCode.UNAUTHORIZED; // 기본 401: 인증 필요

		response.setStatus(code.getStatus().value()); // 401
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// traceId는 필요 시 MDC나 헤더(X-Request-Id)에서 가져오도록 확장하세요.
		String traceId = null;
		String path = request.getRequestURI();

		ErrorResponse body = ErrorResponse.of(code, null, traceId, path);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}