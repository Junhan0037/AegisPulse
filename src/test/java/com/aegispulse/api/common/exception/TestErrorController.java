package com.aegispulse.api.common.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전역 예외 처리기 테스트용 컨트롤러.
 * 각 엔드포인트는 예외 타입별 표준 응답 변환을 검증하기 위해 의도적으로 예외를 발생시킨다.
 */
@RestController
public class TestErrorController {

    @GetMapping("/test/business")
    public String businessError() {
        throw new AegisPulseException(ErrorCode.CONFLICT, "중복된 리소스입니다.");
    }

    @GetMapping("/test/type-mismatch")
    public String typeMismatch(@RequestParam int count) {
        return "count=" + count;
    }

    @GetMapping("/test/unexpected")
    public String unexpectedError() {
        throw new IllegalStateException("unexpected");
    }
}
