package com.aegispulse.api.service.dto;

import com.aegispulse.domain.service.model.ServiceEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Service 등록 요청 DTO.
 * FR-001 입력 스펙(name/upstreamUrl/environment)을 검증한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateServiceRequest {

    @NotBlank(message = "name은 필수입니다.")
    @Pattern(
        regexp = "^[a-z0-9-]{3,50}$",
        message = "name은 영문 소문자/숫자/하이픈 3~50자여야 합니다."
    )
    private String name;

    @NotBlank(message = "upstreamUrl은 필수입니다.")
    @Pattern(
        regexp = "^https?://.+$",
        message = "upstreamUrl은 http 또는 https URL 형식이어야 합니다."
    )
    private String upstreamUrl;

    @NotNull(message = "environment는 필수입니다.")
    private ServiceEnvironment environment;
}
