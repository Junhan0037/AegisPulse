package com.aegispulse.infra.web.trace;

import com.aegispulse.api.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * traceId 필터 테스트 전용 컨트롤러.
 * 요청 속성 traceId를 성공 응답으로 다시 내려 필터 전파 동작을 검증한다.
 */
@RestController
public class TraceIdEchoTestController {

    @GetMapping("/test/trace")
    public ApiResponse<Map<String, String>> trace(HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        return ApiResponse.success(Map.of("traceId", traceId), traceId);
    }
}
