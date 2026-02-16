package com.aegispulse.application.policy;

import com.aegispulse.domain.policy.model.TemplatePolicyProfile;
import com.aegispulse.domain.policy.model.TemplateType;

/**
 * 템플릿 타입을 정책 프로파일로 변환하는 매퍼 포트.
 */
public interface TemplatePolicyMapper {

    TemplatePolicyProfile map(TemplateType templateType);
}
