package com.aegispulse.api.route.dto;

import com.aegispulse.domain.route.model.RouteHttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Route 등록 요청 DTO.
 * FR-002 입력 필드(serviceId/paths/hosts/methods/stripPath)를 검증한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateRouteRequest {

    @NotBlank(message = "serviceId는 필수입니다.")
    private String serviceId;

    @NotEmpty(message = "paths는 최소 1개 이상이어야 합니다.")
    private List<
        @NotBlank(message = "path 값은 비어 있을 수 없습니다.")
        @Pattern(regexp = "^/.*$", message = "path는 '/'로 시작해야 합니다.")
        String> paths;

    private List<
        @NotBlank(message = "host 값은 비어 있을 수 없습니다.")
        String> hosts;

    private List<RouteHttpMethod> methods;

    private Boolean stripPath;
}
