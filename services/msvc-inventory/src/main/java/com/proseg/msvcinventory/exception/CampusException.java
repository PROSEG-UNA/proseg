package com.proseg.msvcinventory.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CampusException extends BaseException {

    public CampusException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static CampusException notFound(String id) {
        return new CampusException(
                HttpStatus.NOT_FOUND,
                "CAMPUS_NOT_FOUND",
                "Campus no encontrado con id: " + id
        );
    }

    public static CampusException duplicateName(String name) {
        return new CampusException(
                HttpStatus.CONFLICT,
                "CAMPUS_DUPLICATE_NAME",
                "Ya existe un campus con el nombre: " + name
        );
    }

    public static CampusException inUse(String name) {
        return new CampusException(
                HttpStatus.BAD_REQUEST,
                "CAMPUS_IN_USE",
                "No se puede eliminar el campus '" + name + "' porque tiene edificios asociados"
        );
    }
}
