package com.sssi.msvc_transport.repository.projection;

import java.util.UUID;

public interface AssignmentOrphanProjection {
    UUID getAssignmentId();

    UUID getDriverId();

    UUID getVehicleId();

    UUID getTourId();

    boolean getMissingDriver();

    boolean getMissingVehicle();

    boolean getMissingTour();
}

