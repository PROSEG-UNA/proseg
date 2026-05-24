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

    public static LocationException duplicateName(String name) {
        return new LocationException(
                HttpStatus.CONFLICT,
                "LOCATION_DUPLICATE_NAME",
                "Ya existe una ubicación con el nombre '" + name + "' en el sitio indicado"
        );
    }

    public static LocationException inUse(String name) {
        return new LocationException(
                HttpStatus.BAD_REQUEST,
                "LOCATION_IN_USE",
                "No se puede eliminar la ubicación '" + name + "' porque tiene activos asociados"
        );
    }
}
