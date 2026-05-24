package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ModelException extends BaseException {

    public ModelException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ModelException notFound(String id) {
        return new ModelException(
                HttpStatus.NOT_FOUND,
                "ASSET_MODEL_NOT_FOUND",
                "Modelo de activo no encontrado con id: " + id
        );
    }

    public static ModelException duplicateName(String name) {
        return new ModelException(
                HttpStatus.CONFLICT,
                "ASSET_MODEL_DUPLICATE_NAME",
                "Ya existe un modelo con el nombre '" + name + "' para la marca indicada"
        );
    }

    public static ModelException inUse(String name) {
        return new ModelException(
                HttpStatus.BAD_REQUEST,
                "ASSET_MODEL_IN_USE",
                "No se puede eliminar el modelo '" + name + "' porque tiene activos asociados"
        );
    }
}
