package com.aegispulse.application.service.command;

import com.aegispulse.domain.service.model.ServiceEnvironment;
import lombok.Builder;
import lombok.Getter;

/**
 * Service 등록 유스케이스 입력 커맨드.
 */
@Getter
@Builder
public class RegisterServiceCommand {

    private final String name;
    private final String upstreamUrl;
    private final ServiceEnvironment environment;
}
