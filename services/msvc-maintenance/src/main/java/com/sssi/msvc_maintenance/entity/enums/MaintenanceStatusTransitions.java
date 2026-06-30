package com.sssi.msvc_maintenance.entity.enums;

import com.sssi.msvc_maintenance.exception.MaintenanceRequestException;

import java.util.Map;
import java.util.Set;

public final class MaintenanceStatusTransitions {

    private static final Map<MaintenanceStatus, Set<MaintenanceStatus>> ALLOWED = Map.of(
            MaintenanceStatus.PENDING, Set.of(MaintenanceStatus.ACCEPTED, MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED),
            MaintenanceStatus.ACCEPTED, Set.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED),
            MaintenanceStatus.COMPLETED, Set.of(),
            MaintenanceStatus.CANCELLED, Set.of()
    );

    private MaintenanceStatusTransitions() {}

    public static boolean canTransition(MaintenanceStatus from, MaintenanceStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void validateOrThrow(MaintenanceStatus from, MaintenanceStatus to) {
        if (!canTransition(from, to)) {
            throw MaintenanceRequestException.invalidStatusTransition(from, to);
        }
    }
}
