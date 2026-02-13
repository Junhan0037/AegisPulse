package com.aegispulse.api.common.exception;

import java.util.Objects;
import lombok.Getter;

/**
 * 도메인/애플리케이션 계층에서 사용하는 공통 비즈니스 예외.
 * ErrorCode를 통해 HTTP 상태와 응답 코드가 일관되게 매핑된다.
 */
@Getter
public class AegisPulseException extends RuntimeException {

    private final ErrorCode errorCode;

    public AegisPulseException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode).getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AegisPulseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
    }
}
