package com.aegispulse.api.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 템플릿 정책 적용 요청 DTO.
 * 서비스 단위 또는 특정 라우트 단위 적용을 위해 serviceId 필수, routeId 선택 입력을 받는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApplyTemplatePolicyRequest {

    @NotBlank(message = "serviceId는 필수입니다.")
    private String serviceId;

    @Pattern(regexp = "^(?!\\s*$).+", message = "routeId는 비어 있을 수 없습니다.")
    private String routeId;
}
