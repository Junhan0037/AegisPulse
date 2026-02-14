package com.aegispulse.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.AegisPulseApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Stage 0 설정 분리 요구사항 검증 테스트.
 * profile 별 application-*.yml 로딩 결과가 정확한지 확인한다.
 */
class ProfileYamlLoadingTest {

    @Test
    @DisplayName("프로필 미지정 시 기본(dev) 설정이 로딩된다")
    void shouldLoadDevProfileByDefault() {
        // 기본 프로필 적용 결과를 검증하기 위해 명시 프로필 없이 컨텍스트를 구동한다.
        try (ConfigurableApplicationContext context = runWithProfiles()) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("aegispulse.env")).isEqualTo("DEV");
        }
    }

    @Test
    @DisplayName("stage 프로필 활성화 시 STAGE 설정이 로딩된다")
    void shouldLoadStageProfile() {
        // stage 프로필 구동 시 application-stage.yml 값을 사용해야 한다.
        try (ConfigurableApplicationContext context = runWithProfiles("stage")) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("aegispulse.env")).isEqualTo("STAGE");
        }
    }

    @Test
    @DisplayName("prod 프로필 활성화 시 PROD 설정이 로딩된다")
    void shouldLoadProdProfile() {
        // prod 프로필 구동 시 application-prod.yml 값을 사용해야 한다.
        try (ConfigurableApplicationContext context = runWithProfiles("prod")) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("aegispulse.env")).isEqualTo("PROD");
        }
    }

    private ConfigurableApplicationContext runWithProfiles(String... profiles) {
        // 테스트 안정성을 위해 내장 웹서버 없이 애플리케이션 컨텍스트만 기동한다.
        return new SpringApplicationBuilder(AegisPulseApplication.class)
            .web(WebApplicationType.NONE)
            .profiles(profiles)
            .run();
    }
}
