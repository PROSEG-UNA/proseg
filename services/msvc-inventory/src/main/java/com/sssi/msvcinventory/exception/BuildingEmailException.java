package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class BuildingEmailException extends BaseException {

    public BuildingEmailException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static BuildingEmailException notFound(String id) {
        return new BuildingEmailException(
                HttpStatus.NOT_FOUND,
                "BUILDING_EMAIL_NOT_FOUND",
                "Correo electrónico no encontrado con id: " + id
        );
    }

    public static BuildingEmailException duplicateEmail(String email) {
        return new BuildingEmailException(
                HttpStatus.CONFLICT,
                "DUPLICATE_BUILDING_EMAIL",
                "El correo '" + email + "' ya está registrado en este edificio"
        );
    }
}