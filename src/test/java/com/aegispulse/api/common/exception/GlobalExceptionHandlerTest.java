package com.aegispulse.api.common.exception;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TestErrorController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("비즈니스 예외는 공통 에러 응답 규약으로 변환된다")
    void shouldConvertBusinessExceptionToStandardErrorResponse() throws Exception {
        mockMvc.perform(get("/test/business").header("X-Trace-Id", "trace-123"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("CONFLICT"))
            .andExpect(jsonPath("$.error.message").value("중복된 리소스입니다."))
            .andExpect(jsonPath("$.error.traceId").value("trace-123"));
    }

    @Test
    @DisplayName("요청 파라미터 타입 오류는 INVALID_REQUEST로 반환된다")
    void shouldReturnInvalidRequestWhenTypeMismatchOccurs() throws Exception {
        mockMvc.perform(get("/test/type-mismatch").param("count", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            // traceId가 누락되지 않도록 비어있지 않은 값만 검증한다.
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 INTERNAL_SERVER_ERROR 규약으로 반환된다")
    void shouldReturnInternalServerErrorForUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
    }
}
