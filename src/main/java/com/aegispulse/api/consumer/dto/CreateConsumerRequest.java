package com.aegispulse.api.consumer.dto;

import com.aegispulse.domain.consumer.model.ConsumerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Consumer 생성 요청 DTO.
 * FR-005의 Consumer 등록 입력값(name/type)을 검증한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateConsumerRequest {

    @NotBlank(message = "name은 필수입니다.")
    @Pattern(
        regexp = "^[a-z0-9-]{3,50}$",
        message = "name은 영문 소문자/숫자/하이픈 3~50자여야 합니다."
    )
    private String name;

    @NotNull(message = "type은 필수입니다.")
    private ConsumerType type;
}
