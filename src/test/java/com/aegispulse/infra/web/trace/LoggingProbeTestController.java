package com.aegispulse.infra.web.trace;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구조화 접근 로그 검증용 테스트 컨트롤러.
 */
@RestController
public class LoggingProbeTestController {

    @GetMapping("/test/logging")
    public String logging() {
        return "ok";
    }
}
