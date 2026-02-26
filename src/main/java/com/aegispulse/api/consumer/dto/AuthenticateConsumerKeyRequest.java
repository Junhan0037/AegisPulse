package com.aegispulse.api.consumer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Consumer API Key 인증 요청 DTO.
 * partner 템플릿이 적용된 서비스/라우트와 consumer 식별자를 입력받는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AuthenticateConsumerKeyRequest {

    @NotBlank(message = "serviceId는 필수입니다.")
    private String serviceId;

    @Pattern(regexp = "^(?!\\s*$).+", message = "routeId는 비어 있을 수 없습니다.")
    private String routeId;

    @NotBlank(message = "consumerId는 필수입니다.")
    private String consumerId;
}
