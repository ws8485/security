package com.cws.config;

import com.cws.security.JwtAuthenticationFilter;
import com.cws.security.handler.RestAuthenticationEntryPoint;
import com.cws.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 애플리케이션 전역 보안 설정. JWT 기반 무상태(stateless) 인증 엔드포인트 접근 제어(권한/인증) CORS 정책 일원화 비밀번호
 * 암호화 및 Dao 인증 구성
 */

@Configuration
@EnableMethodSecurity

public class SecurityConfig {
	/**
	 * 필수 보안 컴포넌트 주입.
	 *
	 * @param jwtAuthenticationFilter JWT를 파싱/검증하여 SecurityContext에 인증 주체를 설정하는 필터
	 * @param userDetailsService      사용자 계정 조회 서비스(로그인/권한 부여에 사용)
	 */
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomUserDetailsService userDetailsService;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
			CustomUserDetailsService userDetailsService) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.userDetailsService = userDetailsService;
	}

	/**
	 * HTTP 보안 체인 구성. 주의: /h2-console/** 공개는 개발 환경 한정 권장. 운영에서는 제거 또는 IP 제한 필요.
	 * /actuator/health 공개는 헬스체크 용도이지만, 추가 엔드포인트 노출 시 반드시 보호 설정 필요.
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,RestAuthenticationEntryPoint restAuthenticationEntryPoint) throws Exception {
		http
				// CSRF 비활성화: 세션을 사용하지 않는 JWT 기반이므로 비활성화
				.csrf(csrf -> csrf.disable())
				// 헤더 조정: H2 콘솔 사용 시 frameOptions 해제(미사용 시 제거 권장)
				.headers(h -> h.frameOptions(f -> f.disable()))
				// 세션 정책: STATELESS로 지정하여 서버 세션 저장소 미사용
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// 인가 규칙:
				.authorizeHttpRequests(auth -> auth
						// • /auth/, /h2-console/, /actuator/health: 전체 허용
						.requestMatchers("/auth/**", "/h2-console/**", "/actuator/health").permitAll()
						// • /admin/**: ROLE_ADMIN 필요
						.requestMatchers("/admin/**").hasRole("ADMIN")
						// • GET /me: 인증 필요
						.requestMatchers(HttpMethod.GET, "/me").authenticated()
						// • 나머지: 인증 필요
						.anyRequest().authenticated())
				// CORS: 별도 Bean과 연동된 정책 적용
				.cors(Customizer.withDefaults())
				// 예외 처리 설정: 인증 실패/미인증(401) 상황에서 커스텀 엔트리포인트로 JSON 에러 응답을 반환합니다.
				// - RestAuthenticationEntryPoint가 호출되어 ErrorResponse 형식으로 응답 바디를 씁니다.
				// - 로그인 필요, 토큰 만료/부재 등 인증 단계의 실패에 대응
				.exceptionHandling(e -> e.authenticationEntryPoint(restAuthenticationEntryPoint))
				// 필터 체인: JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 배치
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * 비밀번호 해싱 알고리즘 설정. BCrypt 사용(가변 비용, 운영 환경에서도 권장) 향후 보안 요구 강화 시 strength 파라미터 조정
	 * 가능
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		// 비밀번호 암호화(운영에서도 BCrypt 권장)
		return new BCryptPasswordEncoder();
	}

	/**
	 * 인증 관리자 구성. DaoAuthenticationProvider를 사용하여 사용자 조회 서비스 + 비밀번호 인코더 기반 인증 수행 주로
	 * 로그인 시 사용되며, JWT 갱신 시에도 유용
	 */
	@Bean
	public AuthenticationManager authenticationManager() {
		// DaoAuthenticationProvider 구성
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return new ProviderManager(provider);
	}

	/**
	 * CORS 정책 구성. 허용 Origin/Method/Header를 환경 변수/설정으로 주입받아 운영-개발 분리 용이 자격
	 * 증명(쿠키/Authorization 헤더) 허용 Authorization 헤더를 클라이언트에서 읽을 수 있도록 노출 파라미터 기본값(미지정
	 * 시): allowed-origins: http://localhost:3000 allowed-methods:
	 * GET,POST,PUT,DELETE,OPTIONS allowed-headers: Authorization,Content-Type 운영
	 * 주의: 와일드카드(*) Origin과 allowCredentials(true)는 함께 사용할 수 없음. 실제 도메인만 명시하여 최소 권한의
	 * 원칙 적용 권장.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins,
			@Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}") List<String> allowedMethods,
			@Value("${cors.allowed-headers:Authorization,Content-Type}") List<String> allowedHeaders) {

		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(allowedMethods);
		config.setAllowedHeaders(allowedHeaders);
		config.setAllowCredentials(true);
		config.setExposedHeaders(List.of("Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		// 모든 경로에 동일 CORS 정책 적용
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}