package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class LocationException extends BaseException {

    public LocationException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static LocationException notFound(String id) {
        return new LocationException(
                HttpStatus.NOT_FOUND,
                "LOCATION_NOT_FOUND",
                "Ubicación no encontrada con id: " + id
        );
    }

    public static LocationException duplicateDescription(String description) {
        return new LocationException(
                HttpStatus.CONFLICT,
                "LOCATION_DUPLICATE_DESCRIPTION",
                "Ya existe una ubicación con la descripción '" + description + "' en el piso indicado"
        );
    }

    public static LocationException inUse(String description) {
        return new LocationException(
                HttpStatus.BAD_REQUEST,
                "LOCATION_IN_USE",
                "No se puede eliminar la ubicación '" + description + "' porque tiene activos asociados"
        );
    }
}
