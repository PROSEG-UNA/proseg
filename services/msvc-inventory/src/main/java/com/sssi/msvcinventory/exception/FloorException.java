package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class FloorException extends BaseException {

    public FloorException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static FloorException notFound(String id) {
        return new FloorException(
                HttpStatus.NOT_FOUND,
                "FLOOR_NOT_FOUND",
                "Piso no encontrado con id: " + id
        );
    }

    public static FloorException duplicateName(String name) {
        return new FloorException(
                HttpStatus.CONFLICT,
                "FLOOR_DUPLICATE_NAME",
                "Ya existe un piso con el nombre '" + name + "' en el edificio indicado"
        );
    }
}
