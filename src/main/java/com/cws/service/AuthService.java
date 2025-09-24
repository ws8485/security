package com.cws.service;

import com.cws.dto.TokenResponse;
import com.cws.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 *
 * 인증(로그인/재발급) 도메인 서비스.
 *
 * 로그인: 자격 증명 검증 후 Access/Refresh 토큰 발급 재발급: 유효한 Refresh 토큰으로 새로운 Access 토큰 발급
 * 보안/운영 주의:
 *
 * 실패 사유는 상세히 노출하지 않고 일관된 메시지로 처리합니다. Refresh 토큰 재사용 공격 방지를 위해 로테이션/블랙리스트 전략을
 * 고려하세요. Access 토큰 만료 시간은 프론트가 갱신 타이밍을 계산할 수 있도록 응답에 포함합니다.
 */
@Service
public class AuthService {
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider tokenProvider;
	private final long accessValiditySeconds;

	/**
	 *
	 * 필수 의존성 주입 및 액세스 토큰 만료(초) 설정.
	 *
	 * @param authenticationManager 사용자명/비밀번호 인증 담당(DaoAuthenticationProvider 기반)
	 * @param tokenProvider         JWT 생성/검증 유틸
	 * @param accessValiditySeconds 액세스 토큰 유효기간(초), 기본 900초(15분)
	 */
	public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
			@Value("${jwt.access-token-validity-seconds:900}") long accessValiditySeconds) {
		this.authenticationManager = authenticationManager;
		this.tokenProvider = tokenProvider;
		this.accessValiditySeconds = accessValiditySeconds;
	}

	/**
	 *
	 * 로그인 처리. AuthenticationManager로 아이디/비밀번호를 검증 성공 시 권한 정보를 포함해 액세스 토큰 생성, 리프레시
	 * 토큰도 함께 발급 예외 처리: 인증 실패/오류는 BadCredentialsException으로 통일하여 상위 계층(전역 예외 처리기)에서
	 * 401로 매핑하기 쉽도록 합니다.
	 */
	public TokenResponse login(String username, String rawPassword) {
		try {
			// 1) 사용자명/비밀번호 인증 시도
			Authentication auth = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(username, rawPassword));
			// 2) 액세스/리프레시 토큰 발급
			String access = tokenProvider.generateAccessToken(username, auth.getAuthorities());
			String refresh = tokenProvider.generateRefreshToken(username);
			// 3) 만료 정보 포함하여 응답
			return new TokenResponse(access, refresh, accessValiditySeconds);
		} catch (Exception e) {
			throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
		}
	}

	/**
	 *
	 * 액세스 토큰 재발급.
	 *
	 * 제출된 리프레시 토큰이 유효한지 검증 리프레시 토큰의 subject(사용자명)로 새로운 액세스 토큰 발급 주의:
	 *
	 * 일반적으로 리프레시 토큰에는 roles를 넣지 않으므로, 필요한 경우 DB 조회로 권한을 가져와 액세스 토큰에 반영하세요. 재사용 방지
	 * 정책(로테이션: 새 리프레시 발급 시 기존 무효화, jti 기록 등)을 도입하면 안전합니다.
	 */
	public TokenResponse refresh(String refreshToken) {
		// 1) 리프레시 토큰 유효성 검증(서명/만료/형식)
		if (!tokenProvider.validate(refreshToken)) {
			throw new BadCredentialsException("리프레시 토큰이 유효하지 않습니다.");
		}
		// 2) 사용자명 추출
		String username = tokenProvider.getUsername(refreshToken);
		// 3) 새로운 액세스 토큰 발급(권한 필요 시 userDetailsService 등으로 재조회 권장)
		// 보통 refresh 토큰에는 roles를 넣지 않습니다. 필요 시 DB 조회로 권한을 불러오세요.
		String newAccess = tokenProvider.generateAccessToken(username, null);
		// 4) 기존 리프레시 토큰 유지(로테이션 전략을 사용한다면 여기서 새 리프레시 발급 및 기존 무효화)
		return new TokenResponse(newAccess, refreshToken, accessValiditySeconds);
	}
}