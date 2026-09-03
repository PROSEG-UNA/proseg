package com.proseg.common.api.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class BuildingException extends BaseException {

    public BuildingException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static BuildingException notFound(String id) {
        return new BuildingException(
                HttpStatus.NOT_FOUND,
                "BUILDING_NOT_FOUND",
                "Edificio no encontrado con id: " + id
        );
    }

    public static BuildingException campusDoesNotMatch(UUID buildingId, UUID campusId) {
        return new BuildingException(
                HttpStatus.BAD_REQUEST,
                "BUILDING_CAMPUS_MISMATCH",
                "El edificio " + buildingId + " no pertenece al campus " + campusId
        );
    }
}
