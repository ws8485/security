package com.cws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * 애플리케이션의 진입점(Main 클래스)입니다.
 *
 * @SpringBootApplication: 컴포넌트 스캔, 자동 설정, 설정 클래스를 한 번에 활성화합니다.
 *
 *                         main 메서드: Spring Boot 애플리케이션을 부트스트랩(시작)합니다.
 */

@SpringBootApplication
public class SecurityApplication {

	/**
	 * 애플리케이션을 시작합니다. IDE에서 실행하거나, 빌드 후 java -jar 로 실행할 수 있습니다.
	 *
	 * @param args 커맨드라인 인자
	 *
	 *             예시) 실행 명령: java -jar app.jar hello world --mode=dev main 메서드의
	 *             args 내용: ["hello", "world", "--mode=dev"]
	 */

	public static void main(String[] args) {
		SpringApplication.run(SecurityApplication.class, args);
	}

}
