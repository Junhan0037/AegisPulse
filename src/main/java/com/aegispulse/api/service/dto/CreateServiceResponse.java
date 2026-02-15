package com.aegispulse.api.service.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Service 등록 성공 응답 DTO.
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateServiceResponse {

    private final String serviceId;
    private final String name;
    private final String environment;
}
