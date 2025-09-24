package com.cws.config;

import com.cws.entity.Role;
import com.cws.entity.User;
import com.cws.repo.RoleRepository;
import com.cws.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 *
 * 애플리케이션 기동 시 초기 데이터(역할/사용자)를 삽입하는 설정 클래스입니다. 관리자/일반 사용자 계정이 없을 경우 자동 생성합니다.
 * 권한(Role)은 먼저 조회하고 없으면 저장하여 영속 엔티티로 만든 뒤, User와 연관을 맺습니다.
 */

@Configuration
public class DataInitializer {

	/**
	 * 애플리케이션 시작 시 한 번 실행되는 초기화 러너를 등록합니다. 트랜잭션 안에서 실행되어 역할/사용자 저장이 하나의 작업으로 처리됩니다.
	 */
	@Bean
	@Transactional
	public CommandLineRunner init(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
		return new CommandLineRunner() {
			@Override public void run(String... args) {
				// 1) 역할(Role) 선저장 또는 조회
				// - "ROLE_ADMIN", "ROLE_USER" 가 없으면 생성하여 저장합니다.
				// - findByName은 Optional을 반환하므로, orElseGet으로 안전하게 처리합니다.
				Role roleAdmin = (Role) roleRepository.findByName("ROLE_ADMIN")
						.orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
				Role roleUser = (Role) roleRepository.findByName("ROLE_USER")
						.orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

				// 2) 관리자 계정 생성
				// - 기존에 "admin" 사용자가 없을 때만 생성합니다.
				// - 비밀번호는 PasswordEncoder로 반드시 암호화하여 저장합니다.
				if (userRepository.findByUsername("admin").isEmpty()) {
					User admin = new User();
					admin.setUsername("admin");
					admin.setPassword(passwordEncoder.encode("password")); // 기본 비번: 데모용, 운영에서 교체 권장
					admin.setRoles(Set.of(roleAdmin, roleUser));// 관리자에게 두 역할 모두 부여
					userRepository.save(admin);
				}

				// 3) 일반 사용자 계정 생성
				// - 기존에 "user" 사용자가 없을 때만 생성합니다.
				if (userRepository.findByUsername("user").isEmpty()) {
					User user = new User();
					user.setUsername("user");
					user.setPassword(passwordEncoder.encode("password")); // 기본 비번: 데모용, 운영에서 교체 권장
					user.setRoles(Set.of(roleUser)); // 일반 사용자 역할만 부여
					userRepository.save(user);
				}

			}
		};
	}

}
