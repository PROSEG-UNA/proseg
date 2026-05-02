package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class BrandException extends BaseException {

    public BrandException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static BrandException notFound(String id) {
        return new BrandException(
                HttpStatus.NOT_FOUND,
                "BRAND_NOT_FOUND",
                "Marca no encontrada con id: " + id
        );
    }

    public static BrandException duplicateName(String name) {
        return new BrandException(
                HttpStatus.CONFLICT,
                "BRAND_DUPLICATE_NAME",
                "Ya existe una marca con el nombre: " + name
        );
    }

    public static BrandException inUse(String id) {
        return new BrandException(
                HttpStatus.BAD_REQUEST,
                "BRAND_IN_USE",
                "No se puede eliminar la marca con id: " + id + " porque tiene modelos de activo asociados"
        );
    }
}
