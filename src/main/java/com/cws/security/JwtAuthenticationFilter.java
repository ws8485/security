package com.cws.security;

import com.cws.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 *
 * JWT를 이용해 요청당 한 번 인증 컨텍스트를 설정하는 필터.
 *
 * 요청 헤더의 Authorization: Bearer 형식에서 토큰을 추출 토큰 유효성 검증 후 사용자 정보를 로드하여
 * SecurityContext에 인증 주체 설정 동작 개요:
 *
 * Authorization 헤더 파싱 → "Bearer " 접두사 확인 토큰 유효성 검증(tokenProvider.validate)
 * 컨텍스트에 인증이 아직 없다면(username 기반으로 UserDetails 로드 후 컨텍스트 설정) 체인 계속 진행 보안 주의:
 *
 * validate는 만료/서명/클레임 등 종합 검증을 수행해야 합니다. 토큰 탈취/무효화 정책(블랙리스트, jti 로테이션)과 함께 사용할
 * 것을 권장합니다. 이미 인증이 있는 경우 재설정하지 않음으로써 불필요한 비용과 덮어쓰기를 방지합니다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailsService userDetailsService;

	/**
	 *
	 * 필수 컴포넌트 주입.
	 *
	 * @param tokenProvider      JWT 생성/검증 및 클레임 파싱 제공자
	 * @param userDetailsService 사용자 정보 로드 서비스
	 */
	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
		this.tokenProvider = tokenProvider;
		this.userDetailsService = userDetailsService;
	}

	/**
	 *
	 * 요청마다 한 번 실행되어 JWT 기반 인증을 시도합니다.
	 *
	 * Authorization 헤더에서 Bearer 토큰을 추출 토큰이 유효하고 현재 컨텍스트에 인증이 없는 경우, 사용자 정보를 로드하여 인증
	 * 객체 설정
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		// 1) Authorization 헤더에서 Bearer 토큰 추출
		String bearer = request.getHeader("Authorization");
		String token = null;
		if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
			token = bearer.substring(7);
		}

		// 2) 토큰 유효성 검증 및 중복 설정 방지
		if (StringUtils.hasText(token) && tokenProvider.validate(token)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			// 3) 토큰에서 사용자명 추출 후 계정 로드
			String username = tokenProvider.getUsername(token);
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			// 4) 인증 토큰 생성(자격 증명은 null, 권한은 UserDetails의 Authorities 사용)
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
					null, userDetails.getAuthorities());
			// 5) 요청 세부정보(원격 주소, 세션 등) 부가 설정
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			// 6) SecurityContext에 인증 저장 → 이후 컨트롤러/메서드 보안에서 인증 주체로 사용
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		// 7) 다음 필터로 체인 계속 진행
		chain.doFilter(request, response);
	}
}