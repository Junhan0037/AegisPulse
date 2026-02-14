package com.aegispulse.infra.web.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 HTTP 요청에 traceId를 부여하고, 헤더/MDC/요청 속성으로 전파한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = TraceIdSupport.resolveOrCreate(request.getHeader(TraceIdSupport.TRACE_ID_HEADER));

        // 컨트롤러/예외 처리기에서 동일 traceId를 재사용할 수 있도록 요청 속성으로 보관한다.
        request.setAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE, traceId);
        // 다운스트림(클라이언트/게이트웨이) 상관관계를 위해 응답 헤더에도 동일 값을 반영한다.
        response.setHeader(TraceIdSupport.TRACE_ID_HEADER, traceId);

        // 로그 상관관계를 위해 MDC에 저장하고, 요청 종료 시 누수 방지를 위해 반드시 정리한다.
        MDC.put(TraceIdSupport.TRACE_ID_MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdSupport.TRACE_ID_MDC_KEY);
        }
    }
}
