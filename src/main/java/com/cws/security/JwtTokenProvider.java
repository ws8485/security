package com.cws.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * JWT(Access/Refresh) 생성 및 검증을 담당하는 유틸리티 컴포넌트.
 *
 * 대칭키(HS256) 기반 서명 사용자명(subject)과 권한(roles) 클레임 처리 토큰 파싱/검증 및 권한 복원 기능 제공 운영/보안
 * 주의:
 *
 * 비밀키는 충분히 길고 난도가 높은 값으로 설정하고, 환경 변수/시크릿 매니저를 이용해 관리하세요. Access/Refresh의 만료 시간을
 * 분리 관리하여 보안과 편의성을 균형 있게 조정하세요. 필요한 경우 jti/issuer/audience 등의 추가 클레임과 키 로테이션
 * 전략을 도입하세요.
 */
@Component
public class JwtTokenProvider {
	private final SecretKey secretKey;
	private final long accessValidityMillis;
	private final long refreshValidityMillis;

	/**
	 *
	 * 설정값을 주입받아 서명 키와 만료 시간을 초기화합니다.
	 *
	 * @param secret         서명용 시크릿(HS256). 충분한 길이와 복잡도를 갖춰야 합니다.
	 * @param accessSeconds  액세스 토큰 유효기간(초) - 기본 900초(15분)
	 * @param refreshSeconds 리프레시 토큰 유효기간(초) - 기본 2,592,000초(30일)
	 */
	public JwtTokenProvider(@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-validity-seconds:900}") long accessSeconds,
			@Value("${jwt.refresh-token-validity-seconds:2592000}") long refreshSeconds) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessValidityMillis = accessSeconds * 1000L;
		this.refreshValidityMillis = refreshSeconds * 1000L;
	}

	/**
	 *
	 * 액세스 토큰 생성.
	 *
	 * subject에 사용자명 설정 roles 클레임에 권한 목록 문자열로 저장 발급 시각/만료 시각 설정
	 */
	public String generateAccessToken(String username, Collection<? extends GrantedAuthority> authorities) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + accessValidityMillis);

		List<String> roles = authorities == null ? List.of()
				: authorities.stream().map(GrantedAuthority::getAuthority).toList();

		return Jwts.builder().setSubject(username).claim("roles", roles).setIssuedAt(now).setExpiration(exp)
				.signWith(secretKey, SignatureAlgorithm.HS256).compact();
	}

	/**
	 *
	 * 리프레시 토큰 생성.
	 *
	 * subject에 사용자명 설정 일반적으로 최소한의 클레임만 포함(roles 미포함)
	 */
	public String generateRefreshToken(String username) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + refreshValidityMillis);

		return Jwts.builder().setSubject(username).setIssuedAt(now).setExpiration(exp)
				.signWith(secretKey, SignatureAlgorithm.HS256).compact();
	}

	/**
	 *
	 * 토큰 유효성 검증. 서명/구조/만료 등 파싱 과정에서 예외가 없으면 true 예외 발생 시 false (상세 구분이 필요하면 예외를 상위로
	 * 전달하도록 변경을 검토)
	 */
	public boolean validate(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 *
	 * 토큰에서 사용자명(subject) 추출.
	 */
	public String getUsername(String token) {
		return parseClaims(token).getBody().getSubject();
	}

	/**
	 *
	 * 토큰에서 권한(roles) 추출. roles 클레임은 문자열 목록을 기대하며, SimpleGrantedAuthority로 변환 클레임이
	 * 없거나 형식이 다르면 빈 목록 반환
	 */
	public Collection<? extends GrantedAuthority> getAuthorities(String token) {
		Claims claims = parseClaims(token).getBody();
		Object roles = claims.get("roles");
		if (roles instanceof List<?> list) {
			return list.stream().filter(Objects::nonNull).map(Object::toString).map(SimpleGrantedAuthority::new)
					.collect(Collectors.toSet());
		}
		return List.of();
	}

	/**
	 *
	 * 토큰 클레임 파싱(서명 검증 포함). 유효하지 않거나 만료된 토큰은 예외를 던집니다.
	 */
	private Jws<Claims> parseClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
	}

}