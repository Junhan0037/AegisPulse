package com.aegispulse.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Admin API 공통 응답 래퍼.
 * 성공/실패 응답을 동일한 envelope로 제공해 클라이언트 파싱 규약을 단순화한다.
 *
 * @param <T> 성공 시 반환되는 payload 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;
    private final String traceId;

    /**
     * 성공 응답을 생성한다.
     */
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(true, data, null, traceId);
    }

    /**
     * 실패 응답을 생성한다.
     * traceId는 ErrorResponse 내부 필드로 전달한다.
     */
    public static <T> ApiResponse<T> failure(ErrorResponse error) {
        return new ApiResponse<>(false, null, Objects.requireNonNull(error), null);
    }
}
