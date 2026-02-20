package com.aegispulse.application.policy;

import com.aegispulse.application.policy.command.ApplyTemplatePolicyCommand;
import com.aegispulse.application.policy.result.ApplyTemplatePolicyResult;

/**
 * 템플릿 정책 적용 유스케이스 계약.
 */
public interface TemplatePolicyApplyUseCase {

    ApplyTemplatePolicyResult apply(ApplyTemplatePolicyCommand command);
}
