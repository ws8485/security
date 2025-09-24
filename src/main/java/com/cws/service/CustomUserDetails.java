package com.cws.service;

import com.cws.entity.Role;
import com.cws.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * 스프링 시큐리티 인증 주체(UserDetails) 구현체.
 *
 * 도메인 엔티티(User)를 감싸서 시큐리티가 요구하는 형태로 노출합니다. 권한(roles)을 SimpleGrantedAuthority로
 * 변환하여 권한 검사(@PreAuthorize 등)에 사용합니다. 운영/보안 참고:
 *
 * Role 네이밍 컨벤션(예: "ROLE_ADMIN")을 시큐리티 설정(hasRole/hasAuthority)과 일치시켜야 합니다. 계정
 * 상태 플래그(활성/잠금/만료)를 정책에 맞게 연동하면 보안 사고를 줄일 수 있습니다.
 */
public class CustomUserDetails implements UserDetails {

	private final User user;

	/**
	 *
	 * 도메인 사용자 엔티티를 주입받아 인증 주체를 구성합니다.
	 *
	 * @param user 인증 대상 사용자
	 */
	public CustomUserDetails(User user) {
		this.user = user;
	}

	/**
	 *
	 * 사용자의 권한 목록을 반환합니다. User.roles → role.name → SimpleGrantedAuthority로 변환 예:
	 * "ROLE_ADMIN" → new SimpleGrantedAuthority("ROLE_ADMIN")
	 */

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Set<Role> roles = user.getRoles();
		return roles.stream().map(Role::getName).map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
	}

	/**
	 *
	 * 비밀번호 해시를 반환합니다. 평문이 아닌 해시(BCrypt 등)여야 합니다.
	 */
	@Override
	public String getPassword() {
		return user.getPassword();
	}

	/**
	 *
	 * 사용자명을 반환합니다. 일반적으로 로그인 ID 혹은 이메일 등 고유 식별자입니다.
	 */
	@Override
	public String getUsername() {
		return user.getUsername();
	}

	/**
	 *
	 * 도메인 사용자 식별자(ID)를 추가로 노출합니다. 컨트롤러/서비스 레이어에서 인증 주체의 내부 식별이 필요할 때 사용합니다.
	 */
	// 필요 시 추가 속성 노출
	public Long getId() {
		return user.getId();
	}

	/**
	 *
	 * 계정 활성화 여부를 반환합니다. UserDetails의 isEnabled와 의미를 일치시키기 위해 그대로 노출합니다.
	 */
	public boolean isEnabled() {
		return user.isEnabled();
	}

	/**
	 *
	 * 계정 만료 여부(비만료=true). 별도 만료 정책이 있다면 user의 필드와 연동하세요.
	 */
	@Override
	public boolean isAccountNonExpired() {
		return true; // 계정 만료 정책이 있다면 연동하세요.
	}

	/**
	 *
	 * 계정 잠금 여부(비잠금=true). 로그인 실패 누적 등에 따른 잠금 정책을 적용하려면 user 필드와 연계하세요.
	 */
	@Override
	public boolean isAccountNonLocked() {
		return true; // 잠금 정책이 있다면 연동하세요.
	}

	/**
	 *
	 * 자격 증명(비밀번호) 만료 여부(비만료=true). 주기적 비밀번호 변경 정책이 있다면 user 필드와 연동하세요.
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return true; // 비밀번호 만료 정책이 있다면 연동하세요.
	}
}