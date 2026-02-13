package com.aegispulse.api.common.response;

import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공통 에러 상세 응답.
 * PRD 규약에 맞춰 code/message/timestamp/traceId를 제공한다.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorResponse {

    private final String code;
    private final String message;
    private final Instant timestamp;
    private final String traceId;

    public static ErrorResponse of(String code, String message, String traceId) {
        // Lombok 생성 생성자를 사용하되, 필수 필드 null 체크는 팩토리 메서드에서 강제한다.
        return new ErrorResponse(
            Objects.requireNonNull(code),
            Objects.requireNonNull(message),
            Instant.now(),
            Objects.requireNonNull(traceId)
        );
    }
}
