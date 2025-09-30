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
 * JWT를 이용해 요청당 한 번 인증 컨텍스트를 설정하는 필터.
 *
 * 동작 개요:
 * - Authorization 헤더 파싱 → "Bearer " 접두사 확인
 * - validateOrThrow로 토큰 검증(만료/서명/형식 오류를 구분해 예외로 위임)
 * - 컨텍스트에 인증이 아직 없다면 사용자 정보 로드 후 인증 주체 설정
 *
 * 보안 주의:
 * - validateOrThrow는 만료/서명/클레임 검증을 수행하며 실패 시 커스텀 예외를 던집니다.
 * - 전역 예외 처리기에서 해당 예외를 401로 표준화하여 응답하세요.
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
	 * 객체 설정
	 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1) Authorization 헤더에서 Bearer 토큰 추출
        String token = resolveToken(request);

        // 2) 이미 인증이 있으면 건너뜀
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        // 3) 토큰이 있으면 검증(실패 시 예외가 던져져 전역 예외 처리기로 이동)
        if (StringUtils.hasText(token)) {
            tokenProvider.validateOrThrow(token); // 변경: boolean validate 제거, 실패 사유 보존

            // 4) 사용자명 및 권한 추출
            String username = tokenProvider.getUsername(token);
            // 권한이 토큰에 들어있다면 토큰에서 복원해서 사용하고,
            // 정책상 반드시 DB에서 최신 권한을 가져와야 한다면 아래 userDetailsService를 사용하세요.
            // 여기서는 DB 로드 방식을 유지합니다.
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5) 인증 토큰 생성(자격 증명 null, 권한은 UserDetails의 Authorities 사용)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 6) 요청 세부정보 부가 설정
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 7) SecurityContext에 인증 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 8) 다음 필터로 체인 계속 진행
        chain.doFilter(request, response);
    }
	/**
	
	 * Authorization 헤더에서 Bearer 토큰을 추출 토큰이 유효하고 현재 컨텍스트에 인증이 없는 경우, 사용자 정보를 로드하여 인증
	 */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}