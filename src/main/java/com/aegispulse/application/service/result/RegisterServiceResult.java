package com.aegispulse.application.service.result;

import lombok.Builder;
import lombok.Getter;

/**
 * Service 등록 유스케이스 출력 모델.
 */
@Getter
@Builder
public class RegisterServiceResult {

    private final String serviceId;
    private final String name;
    private final String environment;
}
