package com.cws.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Aspect
@Component
public class LoggingAspect {

	private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

	// 컨트롤러: com.cws..controller 패키지 내 @RestController에만 적용
	@Around("within(com.cws..controller..*) && @within(org.springframework.web.bind.annotation.RestController)")
	public Object logController(ProceedingJoinPoint pjp) throws Throwable {
		String signature = pjp.getSignature().toShortString();
		String args = maskArgs(pjp.getArgs());
		String traceId = currentTraceId();

		long start = System.currentTimeMillis();
		log.info("[CTRL][START] traceId={} sig={} args={}", traceId, signature, args);
		try {
			Object result = pjp.proceed();
			long took = System.currentTimeMillis() - start;
			log.info("[CTRL][END] traceId={} sig={} took={}ms", traceId, signature, took);
			return result;
		} catch (Exception e) {
			long took = System.currentTimeMillis() - start;
			log.error("[CTRL][ERROR] traceId={} sig={} took={}ms msg={}", traceId, signature, took, e.getMessage(), e);
			throw e;
		}
	}

	// 서비스: com.cws..service 패키지 내 @Service에만 적용
	@Around("within(com.cws..service..*) && @within(org.springframework.stereotype.Service)")
	public Object logService(ProceedingJoinPoint pjp) throws Throwable {
		String signature = pjp.getSignature().toShortString();
		String traceId = currentTraceId();

		long start = System.currentTimeMillis();
		log.debug("[SVC][ENTER] traceId={} sig={}", traceId, signature);
		try {
			Object result = pjp.proceed();
			long took = System.currentTimeMillis() - start;
			log.debug("[SVC][EXIT] traceId={} sig={} took={}ms", traceId, signature, took);
			return result;
		} catch (Exception e) {
			long took = System.currentTimeMillis() - start;
			log.error("[SVC][ERROR] traceId={} sig={} took={}ms msg={}", traceId, signature, took, e.getMessage(), e);
			throw e;
		}
	}

	// 요청 단위 traceId 조회 (필터에서 MDC에 넣는 것을 권장)
	private String currentTraceId() {
		String id = MDC.get("traceId");
		if (id == null) {
			// 과도기용: 아직 필터를 도입하지 않았을 때 대비
			id = UUID.randomUUID().toString();
			MDC.put("traceId", id);
		}
		return id;
	}

	// 민감정보 마스킹
	private String maskArgs(Object[] args) {
		if (args == null || args.length == 0)
			return "[]";
		return Arrays.stream(args).map(this::safeToString).collect(Collectors.joining(", ", "[", "]"));
	}

	private String safeToString(Object obj) {
		if (obj == null)
			return "null";
		if (obj instanceof CharSequence) {
			return maskIfSensitive("value", Objects.toString(obj));
		}
		if (isPrimitiveOrWrapper(obj.getClass())) {
			return String.valueOf(obj);
		}
		// 특정 DTO 특례 처리 예시
		try {
			if (obj.getClass().getName().equals("com.cws.dto.LoginRequest")) {
				// 리플렉션으로 username만 추출하고 password는 마스킹
				try {
					var u = obj.getClass().getMethod("getUsername").invoke(obj);
					return "LoginRequest(username=" + u + ", password=****)";
				} catch (Exception ignore) {
					/* fall-through */ }
			}
		} catch (Exception ignore) {
			/* fall-through */ }

		String s;
		try {
			s = obj.toString();
		} catch (Exception e) {
			s = obj.getClass().getSimpleName();
		}
		if (containsSensitiveKey(s)) {
			return maskIfSensitive("object", s);
		}
		return truncate(s, 500);
	}

	private boolean containsSensitiveKey(String text) {
		String lower = text.toLowerCase();
		return lower.contains("password") || lower.contains("pwd") || lower.contains("token")
				|| lower.contains("authorization") || lower.contains("secret") || lower.contains("credential");
	}

	private String maskIfSensitive(String key, String value) {
		if (containsSensitiveKey(key) || containsSensitiveKey(value)) {
			return key + "=****";
		}
		return truncate(value, 500);
	}

	private String truncate(String s, int max) {
		if (s == null)
			return null;
		return s.length() > max ? s.substring(0, max) + "..." : s;
	}

	private boolean isPrimitiveOrWrapper(Class<?> c) {
		return c.isPrimitive() || c == Byte.class || c == Short.class || c == Integer.class || c == Long.class
				|| c == Float.class || c == Double.class || c == Boolean.class || c == Character.class;
	}
}