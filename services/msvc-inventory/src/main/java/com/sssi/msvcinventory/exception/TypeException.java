package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class TypeException extends BaseException {

    public TypeException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static TypeException notFound(String id) {
        return new TypeException(
                HttpStatus.NOT_FOUND,
                "ASSET_TYPE_NOT_FOUND",
                "Tipo de activo no encontrado con id: " + id
        );
    }

    public static TypeException duplicateName(String name) {
        return new TypeException(
                HttpStatus.CONFLICT,
                "ASSET_TYPE_DUPLICATE_NAME",
                "Ya existe un tipo de activo con el nombre: " + name
        );
    }

    public static TypeException inUse(String name) {
        return new TypeException(
                HttpStatus.BAD_REQUEST,
                "ASSET_TYPE_IN_USE",
                "No se puede eliminar el tipo de activo '" + name + "' porque tiene activos o modelos asociados"
        );
    }
}
