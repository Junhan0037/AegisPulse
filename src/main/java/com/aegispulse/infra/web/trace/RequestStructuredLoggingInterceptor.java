package com.aegispulse.infra.web.trace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 요청/응답 메타데이터를 JSON 구조화 로그로 남긴다.
 * 민감정보 노출 방지를 위해 토큰/바디/쿼리 문자열은 기록하지 않는다.
 */
@Component
public class RequestStructuredLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestStructuredLoggingInterceptor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 처리 시간 측정을 위해 시작 시각(ns)을 저장한다.
        request.setAttribute(TraceIdSupport.REQUEST_START_NANOS_ATTRIBUTE, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        @Nullable Exception exception
    ) {
        long durationMs = resolveDurationMs(request);
        String traceId = resolveTraceId(request);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "http_access");
        payload.put("timestamp", Instant.now().toString());
        payload.put("traceId", traceId);
        payload.put("method", request.getMethod());
        payload.put("path", request.getRequestURI());
        payload.put("status", response.getStatus());
        payload.put("durationMs", durationMs);
        payload.put("clientIp", resolveClientIp(request));
        payload.put("userAgent", request.getHeader("User-Agent"));
        payload.put("outcome", exception == null ? "SUCCESS" : "ERROR");

        if (exception != null) {
            // 예외 메시지 원문 대신 타입만 남겨 민감정보 노출 가능성을 줄인다.
            payload.put("errorType", exception.getClass().getSimpleName());
        }

        log.info(toJson(payload));
    }

    private long resolveDurationMs(HttpServletRequest request) {
        Object startNanos = request.getAttribute(TraceIdSupport.REQUEST_START_NANOS_ATTRIBUTE);
        if (startNanos instanceof Long start) {
            return (System.nanoTime() - start) / 1_000_000L;
        }
        return -1L;
    }

    private String resolveTraceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        if (traceId instanceof String value && TraceIdSupport.isValid(value)) {
            return value;
        }
        return TraceIdSupport.generate();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            // 직렬화 실패 시에도 핵심 식별자는 평문으로 남겨 추적성을 유지한다.
            return "{\"event\":\"http_access\",\"serializationError\":true,\"traceId\":\""
                + payload.get("traceId")
                + "\"}";
        }
    }
}
