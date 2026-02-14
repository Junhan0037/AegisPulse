package com.aegispulse.api.common.exception;

import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.api.common.response.ErrorResponse;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * API 전역 예외 처리기.
 * 모든 실패를 PRD 표준 에러 바디(ApiResponse<ErrorResponse>)로 변환한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AegisPulseException.class)
    public ResponseEntity<ApiResponse<Void>> handleAegisPulseException(
        AegisPulseException exception,
        HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        return buildErrorResponse(
            errorCode.getHttpStatus(),
            errorCode.getCode(),
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.INVALID_REQUEST.getCode(),
            resolveBadRequestMessage(exception),
            request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException exception,
        HttpServletRequest request
    ) {
        return buildErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
        ResponseStatusException exception,
        HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return buildErrorResponse(
            status,
            toErrorCode(status),
            Objects.requireNonNullElse(exception.getReason(), status.getReasonPhrase()),
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
        Exception exception,
        HttpServletRequest request
    ) {
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
            request
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
        HttpStatus status,
        String code,
        String message,
        HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        ErrorResponse errorResponse = ErrorResponse.of(code, message, traceId);
        return ResponseEntity.status(status).body(ApiResponse.failure(errorResponse));
    }

    private String resolveBadRequestMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException e && e.getBindingResult().hasFieldErrors()) {
            return Objects.requireNonNullElse(
                e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage(),
                ErrorCode.INVALID_REQUEST.getDefaultMessage()
            );
        }

        if (exception instanceof BindException e && e.getBindingResult().hasFieldErrors()) {
            return Objects.requireNonNullElse(
                e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage(),
                ErrorCode.INVALID_REQUEST.getDefaultMessage()
            );
        }

        return ErrorCode.INVALID_REQUEST.getDefaultMessage();
    }

    private String resolveTraceId(HttpServletRequest request) {
        // Stage 0 traceId 필터 적용 전에도 에러 응답에 traceId가 항상 존재하도록 방어적으로 생성한다.
        String attributeTraceId = (String) request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        if (StringUtils.hasText(attributeTraceId)) {
            return attributeTraceId;
        }

        String headerTraceId = request.getHeader(TraceIdSupport.TRACE_ID_HEADER);
        if (StringUtils.hasText(headerTraceId)) {
            return headerTraceId;
        }

        String mdcTraceId = MDC.get(TraceIdSupport.TRACE_ID_MDC_KEY);
        if (StringUtils.hasText(mdcTraceId)) {
            return mdcTraceId;
        }

        return TraceIdSupport.generate();
    }

    private String toErrorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.INVALID_REQUEST.getCode();
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED.getCode();
            case FORBIDDEN -> ErrorCode.FORBIDDEN.getCode();
            case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND.getCode();
            case CONFLICT -> ErrorCode.CONFLICT.getCode();
            case INTERNAL_SERVER_ERROR -> ErrorCode.INTERNAL_SERVER_ERROR.getCode();
            default -> status.name();
        };
    }
}
