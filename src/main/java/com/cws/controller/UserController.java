package com.cws.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
	// 인증된 사용자 정보 확인용 엔드포인트
	@GetMapping("/me")
	public Object me(Authentication authentication) {
		return new Object() {
			public final String username = authentication.getName();
			public final Object authorities = authentication.getAuthorities();
		};
	}

	// 관리자 전용 예시 엔드포인트 (SecurityConfig에서 ROLE_ADMIN 필요)
	@GetMapping("/admin/panel")
	public String adminPanel() {
		return "관리자 전용 패널";
	}
}