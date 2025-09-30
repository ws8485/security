package com.cws.security;

import com.cws.common.exception.TokenExpiredException;
import com.cws.common.exception.TokenInvalidException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
*
* JWT(Access/Refresh) 생성 및 검증을 담당하는 유틸리티 컴포넌트.
*
*/
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessValidityMillis;
    private final long refreshValidityMillis;
    private final String issuer;
    private final String audience;
    private final String kid; // 단일 키 기준 고정값. 추후 로테이션 시 v2 등으로 확장

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds:900}") long accessSeconds,
            @Value("${jwt.refresh-token-validity-seconds:2592000}") long refreshSeconds,
            @Value("${jwt.issuer:cws-auth}") String issuer,
            @Value("${jwt.audience:cws-api}") String audience,
            @Value("${jwt.kid:v1}") String kid
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessValidityMillis = accessSeconds * 1000L;
        this.refreshValidityMillis = refreshSeconds * 1000L;
        this.issuer = issuer;
        this.audience = audience;
        this.kid = kid;
    }

    public String generateAccessToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessValidityMillis);
        List<String> roles = authorities == null ? List.of()
                : authorities.stream().map(GrantedAuthority::getAuthority).toList();

        return Jwts.builder()
                .setHeaderParam("kid", kid)
                .setIssuer(issuer)
                .setAudience(audience)
                .setSubject(username)
                .setId(UUID.randomUUID().toString()) // jti
                .setIssuedAt(now)
                .setNotBefore(now)
                .setExpiration(exp)
                .claim("roles", roles)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshValidityMillis);

        return Jwts.builder()
                .setHeaderParam("kid", kid)
                .setIssuer(issuer)
                .setAudience(audience)
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setNotBefore(now)
                .setExpiration(exp)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 기존 boolean validate는 그대로 두되, 내부적으로 parse를 시도 */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 실패 사유를 구분해서 예외로 던지는 메서드 */
    public void validateOrThrow(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("토큰이 만료되었습니다.", e);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            throw new TokenInvalidException("서명 검증에 실패했습니다.", e);
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            throw new TokenInvalidException("지원되지 않거나 잘못된 토큰 형식입니다.", e);
        } catch (IllegalArgumentException e) {
            throw new TokenInvalidException("빈 토큰이거나 잘못된 입력입니다.", e);
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    public Collection<? extends GrantedAuthority> getAuthorities(String token) {
        Claims claims = parseClaims(token).getBody();
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }
        return List.of();
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .requireIssuer(issuer)
                .requireAudience(audience)
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
    }
}