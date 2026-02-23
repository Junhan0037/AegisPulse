package com.aegispulse.api.common.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * API 공통 에러 코드.
 * HTTP 상태와 도메인 에러 코드를 함께 관리해 응답 규약을 일관되게 유지한다.
 */
@Getter
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값 검증에 실패했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청이 현재 리소스 상태와 충돌합니다."),
    SERVICE_DUPLICATED(HttpStatus.CONFLICT, "SERVICE_DUPLICATED", "동일 환경에 이미 존재하는 서비스 이름입니다."),
    CONSUMER_DUPLICATED(HttpStatus.CONFLICT, "CONSUMER_DUPLICATED", "이미 존재하는 consumer 이름입니다."),
    ROUTE_CONFLICT(HttpStatus.CONFLICT, "ROUTE_CONFLICT", "동일 서비스 내 충돌하는 라우트입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
