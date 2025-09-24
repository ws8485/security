package com.cws.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 *
 * Jackson ObjectMapper 전역 설정. java.time 모듈 활성화로 날짜/시간을 ISO-8601 문자열로 직렬화(타임스탬프
 * 숫자 비활성화) 역직렬화 시 알 수 없는 필드를 무시해 입력 스키마 변화에 견고하게 대응 null 필드는 응답에서 제외해 페이로드를
 * 간결하게 유지 운영/테스트 참고: 저수준 파서 옵션(JsonReadFeature/StreamReadFeature)이나 커스텀
 * dateFormat은 환경에 따라 충돌을 유발할 수 있어 본 설정에서는 제외했습니다. 필요 시 한 항목씩 추가하며 테스트를 통과하는지
 * 확인하세요.
 */
@Configuration
public class ObjectMapperConfig {
	/**
	 * 커스터마이징된 ObjectMapper Bean 등록. - Spring MVC 메시지 컨버터가 이 ObjectMapper를 사용합니다. -
	 * Spring Boot 기본 설정과의 충돌을 줄이기 위해 필수 옵션만 적용합니다.
	 */
	@Bean
	@Primary
	public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
	    return builder
				// java.time(LocalDateTime 등) 및 Optional 지원
				.modules(new JavaTimeModule(), new Jdk8Module())

				// 날짜를 숫자 타임스탬프가 아닌 ISO-8601 문자열로 처리
				.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				//.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 숫자 직렬화

				// 빈 객체 직렬화 에러 방지(프록시/레코드 등 특수 타입 대응)
				.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

				// 알 수 없는 필드는 무시하여 역직렬화 안정성 확보
				.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

				// null 필드는 응답에서 제외해 페이로드 축소
				.serializationInclusion(JsonInclude.Include.NON_NULL)

				.build();
	}
}