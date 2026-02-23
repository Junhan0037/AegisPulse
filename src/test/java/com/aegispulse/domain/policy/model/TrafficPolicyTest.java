package com.aegispulse.domain.policy.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrafficPolicyTest {

    @Test
    @DisplayName("FR-004 허용 최소 경계값으로 TrafficPolicy를 생성할 수 있다")
    void shouldCreateTrafficPolicyWithMinimumBoundaryValues() {
        TrafficPolicy trafficPolicy = TrafficPolicy.of(
            10,
            500,
            500,
            500,
            0,
            100
        );

        assertThat(trafficPolicy.getRateLimitPerMinute()).isEqualTo(10);
        assertThat(trafficPolicy.getTimeoutConnectMs()).isEqualTo(500);
        assertThat(trafficPolicy.getTimeoutReadMs()).isEqualTo(500);
        assertThat(trafficPolicy.getTimeoutWriteMs()).isEqualTo(500);
        assertThat(trafficPolicy.getRetryCount()).isEqualTo(0);
        assertThat(trafficPolicy.getRetryBackoffMs()).isEqualTo(100);
    }

    @Test
    @DisplayName("FR-004 허용 최대 경계값으로 TrafficPolicy를 생성할 수 있다")
    void shouldCreateTrafficPolicyWithMaximumBoundaryValues() {
        TrafficPolicy trafficPolicy = TrafficPolicy.of(
            10_000,
            10_000,
            10_000,
            10_000,
            5,
            100
        );

        assertThat(trafficPolicy.getRateLimitPerMinute()).isEqualTo(10_000);
        assertThat(trafficPolicy.getTimeoutConnectMs()).isEqualTo(10_000);
        assertThat(trafficPolicy.getTimeoutReadMs()).isEqualTo(10_000);
        assertThat(trafficPolicy.getTimeoutWriteMs()).isEqualTo(10_000);
        assertThat(trafficPolicy.getRetryCount()).isEqualTo(5);
        assertThat(trafficPolicy.getRetryBackoffMs()).isEqualTo(100);
    }

    @Test
    @DisplayName("Rate Limit가 커스터마이징 범위를 벗어나면 예외를 던진다")
    void shouldThrowWhenRateLimitIsOutOfRange() {
        assertThatThrownBy(() -> TrafficPolicy.of(9, 500, 500, 500, 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rateLimitPerMinute");

        assertThatThrownBy(() -> TrafficPolicy.of(10_001, 500, 500, 500, 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rateLimitPerMinute");
    }

    @Test
    @DisplayName("connect/read/write Timeout이 커스터마이징 범위를 벗어나면 예외를 던진다")
    void shouldThrowWhenTimeoutIsOutOfRange() {
        assertThatThrownBy(() -> TrafficPolicy.of(10, 499, 500, 500, 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeoutConnectMs");

        assertThatThrownBy(() -> TrafficPolicy.of(10, 500, 499, 500, 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeoutReadMs");

        assertThatThrownBy(() -> TrafficPolicy.of(10, 500, 500, 499, 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeoutWriteMs");

        assertThatThrownBy(() -> TrafficPolicy.of(10, 10_001, 500, 500, 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeoutConnectMs");

        assertThatThrownBy(() -> TrafficPolicy.of(10, 500, 10_001, 500, 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeoutReadMs");

        assertThatThrownBy(() -> TrafficPolicy.of(10, 500, 500, 10_001, 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeoutWriteMs");
    }

    @Test
    @DisplayName("Retry count가 커스터마이징 범위를 벗어나면 예외를 던진다")
    void shouldThrowWhenRetryCountIsOutOfRange() {
        assertThatThrownBy(() -> TrafficPolicy.of(10, 500, 500, 500, -1, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("retryCount");

        assertThatThrownBy(() -> TrafficPolicy.of(10, 500, 500, 500, 6, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("retryCount");
    }

    @Test
    @DisplayName("Retry backoff은 100ms 고정값이 아니면 예외를 던진다")
    void shouldThrowWhenRetryBackoffIsNotFixedValue() {
        assertThatThrownBy(() -> TrafficPolicy.of(10, 500, 500, 500, 0, 99))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("retryBackoffMs");

        assertThatThrownBy(() -> TrafficPolicy.of(10, 500, 500, 500, 0, 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("retryBackoffMs");
    }
}
