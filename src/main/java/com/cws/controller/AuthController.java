package com.cws.controller;

import com.cws.dto.LoginRequest;
import com.cws.dto.RefreshRequest;
import com.cws.dto.TokenResponse;
import com.cws.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 * 인증 관련 엔드포인트를 제공하는 컨트롤러.
 *
 * 로그인(액세스/리프레시 토큰 발급) 리프레시 토큰을 이용한 액세스 토큰 재발급
 *
 * 보안/운영 참고: 이 컨트롤러의 경로는 보안 설정에서 공개되어야 합니다. (예: /auth/** permitAll) 요청 DTO에 Bean
 * Validation(@Valid)을 적용하여 입력값을 1차 필터링합니다. 응답으로는 토큰 페이로드(TokenResponse)를 반환하며,
 * 민감 정보 노출이 없도록 유지합니다.
 */

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * 사용자 로그인 엔드포인트. 자격 증명(아이디/비밀번호)을 검증하고, 성공 시 액세스/리프레시 토큰을 발급합니다. 요청 예: POST
	 * /auth/login { "username": "user@example.com", "password": "plain-password" }
	 * 검증/예외:
	 *
	 * @Valid: 필수 필드 누락/형식 오류 시 400 Bad Request 인증 실패 시 도메인 레벨 예외를 401 Unauthorized
	 *         혹은 400으로 매핑하는 것을 권장합니다. 보안 주의: 에러 메시지는 아이디/비밀번호 구분 없이 통합 메시지 사용을
	 *         권장합니다. 로그인 성공 시 토큰은 HTTPS 환경에서만 전달/저장하도록 안내하세요.
	 */
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
		TokenResponse tokens = authService.login(request.getUsername(), request.getPassword());
		return ResponseEntity.ok(tokens);
	}

	/**
	 * 토큰 재발급 엔드포인트. 유효한 리프레시 토큰을 제출하면 새로운 액세스(및 필요 시 리프레시) 토큰을 발급합니다. 요청 예: POST
	 * /auth/refresh { "refreshToken": "eyJhbGciOi..." } 검증/예외:
	 *
	 * @Valid로 형식 검증. 토큰 만료/위변조/블랙리스트 등의 경우 401 또는 403으로 매핑 권장. 재사용 공격 방지: 리프레시 토큰
	 *         로테이션 및 사용 기록(토큰 ID, jti) 검증 고려. 운영 주의: 재발급 빈도 제한(레이트 리밋) 적용을 검토하세요.
	 *         클라이언트는 기존 액세스 토큰 만료가 임박했을 때만 호출하도록 가이드합니다.
	 */
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
		TokenResponse tokens = authService.refresh(request.getRefreshToken());
		return ResponseEntity.ok(tokens);
	}
}