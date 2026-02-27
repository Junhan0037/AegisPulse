package com.aegispulse.api.alert.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 알림 목록 조회 응답 DTO.
 */
@Getter
@Builder
public class QueryAlertsResponse {

    private final List<AlertItemResponse> alerts;
}
