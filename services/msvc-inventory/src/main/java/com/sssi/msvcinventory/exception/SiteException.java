package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class SiteException extends BaseException {

    public SiteException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static SiteException notFound(String id) {
        return new SiteException(
                HttpStatus.NOT_FOUND,
                "SITE_NOT_FOUND",
                "Sitio no encontrado con id: " + id
        );
    }

    public static SiteException duplicateName(String name) {
        return new SiteException(
                HttpStatus.CONFLICT,
                "SITE_DUPLICATE_NAME",
                "Ya existe un sitio con el nombre: " + name
        );
    }

    public static SiteException inUse(String name) {
        return new SiteException(
                HttpStatus.BAD_REQUEST,
                "SITE_IN_USE",
                "No se puede eliminar el sitio '" + name + "' porque tiene ubicaciones asociadas"
        );
    }
}
