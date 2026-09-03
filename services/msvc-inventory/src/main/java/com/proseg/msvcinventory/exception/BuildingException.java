package com.proseg.msvcinventory.exception;

import com.proseg.common.api.exception.BaseException;
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

    public static BuildingException duplicateName(String name) {
        return new BuildingException(
                HttpStatus.CONFLICT,
                "BUILDING_DUPLICATE_NAME",
                "Ya existe un edificio con el nombre '" + name + "' en el campus indicado"
        );
    }

    public static BuildingException inUse(String name) {
        return new BuildingException(
                HttpStatus.BAD_REQUEST,
                "BUILDING_IN_USE",
                "No se puede eliminar el edificio '" + name + "' porque tiene pisos asociados"
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
