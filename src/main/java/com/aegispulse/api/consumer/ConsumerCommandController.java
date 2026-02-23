package com.aegispulse.api.consumer;

import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.api.consumer.dto.CreateConsumerRequest;
import com.aegispulse.api.consumer.dto.CreateConsumerResponse;
import com.aegispulse.application.consumer.ConsumerRegistrationUseCase;
import com.aegispulse.application.consumer.command.RegisterConsumerCommand;
import com.aegispulse.application.consumer.result.RegisterConsumerResult;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage 3 Consumer 생성 API 컨트롤러.
 * 입력 검증/응답 포맷/traceId 전파 같은 인터페이스 책임만 수행한다.
 */
@RestController
@RequestMapping("/api/v1/consumers")
@RequiredArgsConstructor
public class ConsumerCommandController {

    private final ConsumerRegistrationUseCase consumerRegistrationUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateConsumerResponse>> createConsumer(
        @Valid @RequestBody CreateConsumerRequest request,
        HttpServletRequest httpServletRequest
    ) {
        RegisterConsumerCommand command = RegisterConsumerCommand.builder()
            .name(request.getName().trim())
            .type(request.getType())
            .build();

        RegisterConsumerResult result = consumerRegistrationUseCase.register(command);
        CreateConsumerResponse response = CreateConsumerResponse.builder()
            .consumerId(result.getConsumerId())
            .name(result.getName())
            .type(result.getType())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, resolveTraceId(httpServletRequest)));
    }

    private String resolveTraceId(HttpServletRequest request) {
        // 필터 체인이 비정상 동작해도 응답 traceId를 보장해 클라이언트 디버깅 가능성을 유지한다.
        Object traceIdAttribute = request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        if (traceIdAttribute instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId;
        }
        return TraceIdSupport.generate();
    }
}
