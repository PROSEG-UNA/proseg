package com.proseg.msvc_transport.specification;

import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.VehicleType;

import java.util.List;
import java.util.Map;

public final class DisplayLabels {

    private DisplayLabels() {}

    public static final Map<String, String> DRIVER_STATUS = Map.of(
            "ACTIVE", "Activo",
            "INACTIVE", "Inactivo"
    );

    public static final Map<VehicleType, String> VEHICLE_TYPE = Map.of(
            VehicleType.MICROBUS, "Microbús",
            VehicleType.SEDAN, "Sedán",
            VehicleType.ALL_TERRAIN, "Todo Terreno",
            VehicleType.SPECIAL, "Especial",
            VehicleType.PICK_UP, "Pick-Up",
            VehicleType.STATION_WAGON, "Station Wagon"
    );

    public static final Map<Boolean, String> VEHICLE_AVAILABILITY = Map.of(
            true, "Disponible",
            false, "No disponible"
    );

    public static final Map<TourProcessingDraftStatus, String> DRAFT_STATUS = Map.of(
            TourProcessingDraftStatus.DRAFT, "Borrador",
            TourProcessingDraftStatus.CLOSED, "Terminada",
            TourProcessingDraftStatus.REGISTERED, "Registrado"
    );

    public static final Map<TourApprovalStatus, String> APPROVAL_STATUS = Map.of(
            TourApprovalStatus.APPROVED, "Aprobada",
            TourApprovalStatus.NOT_APPROVED, "No aprobada"
    );

    public static final List<String> MONTH_NAMES = List.of(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"
    );
}
