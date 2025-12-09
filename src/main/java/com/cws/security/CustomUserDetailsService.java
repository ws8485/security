package com.cws.security;

import com.cws.entity.User;
import com.cws.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 *
 * 스프링 시큐리티용 사용자 조회 서비스 구현체.
 * 사용자명(로그인 ID)로 사용자 정보를 조회하여 UserDetails 형태로 반환합니다. 인증 과정(예:
 * DaoAuthenticationProvider)에서 비밀번호 검증 전에 사용자 로딩에 사용됩니다. 운영/보안 참고:
 * 존재하지 않는 사용자에 대한 메시지는 보안상 상세 노출을 피하되, 서버 로그에는 식별 가능하도록 남기는 것을 권장합니다. 계정
 * 상태(활성/잠금/만료)는 CustomUserDetails에서 플래그와 연동하여 정책을 반영하세요.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	/**
	 *
	 * 사용자 저장소 주입.
	 *
	 * @param userRepository 사용자 엔티티 조회 리포지토리
	 */
	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 *
	 * 사용자명으로 사용자 정보를 로드합니다. 성공: 도메인 User를 CustomUserDetails로 감싸 반환 실패:
	 * UsernameNotFoundException 발생(전역 예외 처리기에서 401 또는 보안상 통합 메시지로 매핑 권장)
	 *
	 * @param username 로그인 식별자(예: 아이디/이메일)
	 * @return CustomUserDetails (UserDetails 구현체)
	 * @throws UsernameNotFoundException 사용자가 존재하지 않을 때
	 */
	@Override
	public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
		return new CustomUserDetails(user);
	}
}