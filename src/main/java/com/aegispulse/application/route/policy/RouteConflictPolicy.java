package com.aegispulse.application.route.policy;

import com.aegispulse.domain.route.model.ManagedRoute;
import com.aegispulse.domain.route.model.RouteHttpMethod;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * FR-002 충돌 규칙(path + host + method 교집합)을 캡슐화한다.
 */
@Component
public class RouteConflictPolicy {

    public boolean hasConflict(ManagedRoute candidate, List<ManagedRoute> existingRoutes) {
        for (ManagedRoute existing : existingRoutes) {
            if (pathsOverlap(candidate.getPaths(), existing.getPaths())
                && hostsOverlap(candidate.getHosts(), existing.getHosts())
                && methodsOverlap(candidate.getMethods(), existing.getMethods())) {
                return true;
            }
        }
        return false;
    }

    private boolean pathsOverlap(List<String> left, List<String> right) {
        return intersects(left, right);
    }

    private boolean hostsOverlap(List<String> left, List<String> right) {
        // hosts 미입력은 와일드카드(전체 host 허용) 의미로 처리한다.
        if (left.isEmpty() || right.isEmpty()) {
            return true;
        }
        return intersects(left, right);
    }

    private boolean methodsOverlap(List<RouteHttpMethod> left, List<RouteHttpMethod> right) {
        // methods 미입력은 와일드카드(전체 method 허용) 의미로 처리한다.
        if (left.isEmpty() || right.isEmpty()) {
            return true;
        }
        return intersects(left, right);
    }

    private <T> boolean intersects(List<T> left, List<T> right) {
        Set<T> lookup = new HashSet<>(left);
        for (T value : right) {
            if (lookup.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
