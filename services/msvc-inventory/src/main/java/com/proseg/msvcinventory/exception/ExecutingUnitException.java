package com.proseg.msvcinventory.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ExecutingUnitException extends BaseException {

    public ExecutingUnitException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ExecutingUnitException notFound(String id) {
        return new ExecutingUnitException(
                HttpStatus.NOT_FOUND,
                "EXECUTING_UNIT_NOT_FOUND",
                "Unidad ejecutora no encontrada con id: " + id
        );
    }

    public static ExecutingUnitException duplicateName(String name) {
        return new ExecutingUnitException(
                HttpStatus.CONFLICT,
                "EXECUTING_UNIT_DUPLICATE_NAME",
                "Ya existe una unidad ejecutora con el nombre: " + name
        );
    }

    public static ExecutingUnitException inUse(String name) {
        return new ExecutingUnitException(
                HttpStatus.BAD_REQUEST,
                "EXECUTING_UNIT_IN_USE",
                "No se puede eliminar la unidad ejecutora '" + name + "' porque tiene activos asociados"
        );
    }
}
