package com.aegispulse.api.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    @DisplayName("성공 응답은 success/data/traceId를 포함한다")
    void shouldCreateSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ok", "trace-1");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("ok");
        assertThat(response.getTraceId()).isEqualTo("trace-1");
        assertThat(response.getError()).isNull();
    }

    @Test
    @DisplayName("실패 응답은 success=false와 error를 포함한다")
    void shouldCreateFailureResponse() {
        ErrorResponse errorResponse = ErrorResponse.of("INVALID_REQUEST", "잘못된 요청입니다.", "trace-2");
        ApiResponse<Void> response = ApiResponse.failure(errorResponse);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo(errorResponse);
        assertThat(response.getData()).isNull();
        assertThat(response.getTraceId()).isNull();
    }
}
