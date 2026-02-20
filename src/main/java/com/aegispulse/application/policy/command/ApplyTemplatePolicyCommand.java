package com.aegispulse.application.policy.command;

import com.aegispulse.domain.policy.model.TemplateType;
import lombok.Builder;
import lombok.Getter;

/**
 * 템플릿 정책 적용 유스케이스 입력 커맨드.
 */
@Getter
@Builder
public class ApplyTemplatePolicyCommand {

    private final String serviceId;
    private final String routeId;
    private final TemplateType templateType;
}
