package com.aegispulse.domain.policy.model;

import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 템플릿 정책 적용 이력을 나타내는 바인딩 도메인 모델.
 * PRD 데이터 모델의 PolicyBinding(serviceId, routeId, templateType, policySnapshot, version)을 표현한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyBinding {

    private final String id;
    private final String serviceId;
    private final String routeId;
    private final TemplateType templateType;
    private final String policySnapshot;
    private final int version;
    private final Instant createdAt;

    /**
     * 신규 템플릿 적용 바인딩을 생성한다.
     * createdAt은 생성 시각으로 고정하며 version은 1 이상이어야 한다.
     */
    public static PolicyBinding newBinding(
        String id,
        String serviceId,
        String routeId,
        TemplateType templateType,
        String policySnapshot,
        int version
    ) {
        validateVersion(version);
        return PolicyBinding.builder()
            .id(Objects.requireNonNull(id, "id는 필수입니다."))
            .serviceId(Objects.requireNonNull(serviceId, "serviceId는 필수입니다."))
            .routeId(routeId)
            .templateType(Objects.requireNonNull(templateType, "templateType은 필수입니다."))
            .policySnapshot(Objects.requireNonNull(policySnapshot, "policySnapshot은 필수입니다."))
            .version(version)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * 영속 계층에서 기존 바인딩을 복원한다.
     */
    public static PolicyBinding restore(
        String id,
        String serviceId,
        String routeId,
        TemplateType templateType,
        String policySnapshot,
        int version,
        Instant createdAt
    ) {
        validateVersion(version);
        return PolicyBinding.builder()
            .id(Objects.requireNonNull(id, "id는 필수입니다."))
            .serviceId(Objects.requireNonNull(serviceId, "serviceId는 필수입니다."))
            .routeId(routeId)
            .templateType(Objects.requireNonNull(templateType, "templateType은 필수입니다."))
            .policySnapshot(Objects.requireNonNull(policySnapshot, "policySnapshot은 필수입니다."))
            .version(version)
            .createdAt(Objects.requireNonNull(createdAt, "createdAt은 필수입니다."))
            .build();
    }

    private static void validateVersion(int version) {
        if (version < 1) {
            throw new IllegalArgumentException("version은 1 이상이어야 합니다.");
        }
    }
}
