package com.aegispulse.infra.web.trace;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * traceId 전파에 필요한 공통 상수/유틸리티를 제공한다.
 * 입력 헤더는 최소한의 형식 검증을 거쳐 로그 오염 가능성을 줄인다.
 */
public final class TraceIdSupport {

    public static final String TRACE_ID_ATTRIBUTE = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String REQUEST_START_NANOS_ATTRIBUTE = "requestStartNanos";

    // 로그/헤더 전파 용도로 안전한 문자셋만 허용한다.
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    private TraceIdSupport() {
    }

    /**
     * 외부 헤더 traceId가 유효하면 그대로 사용하고, 아니면 새 값을 생성한다.
     */
    public static String resolveOrCreate(String incomingTraceId) {
        if (isValid(incomingTraceId)) {
            return incomingTraceId;
        }
        return generate();
    }

    /**
     * 내부 표준 traceId를 생성한다.
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 전파 가능한 traceId 형식인지 검증한다.
     */
    public static boolean isValid(String traceId) {
        return StringUtils.hasText(traceId) && TRACE_ID_PATTERN.matcher(traceId).matches();
    }
}
