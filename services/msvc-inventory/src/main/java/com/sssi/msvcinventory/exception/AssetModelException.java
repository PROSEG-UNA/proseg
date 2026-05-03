package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AssetModelException extends BaseException {

    public AssetModelException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AssetModelException notFound(String id) {
        return new AssetModelException(
                HttpStatus.NOT_FOUND,
                "ASSET_MODEL_NOT_FOUND",
                "Modelo de activo no encontrado con id: " + id
        );
    }

    public static AssetModelException duplicateName(String name) {
        return new AssetModelException(
                HttpStatus.CONFLICT,
                "ASSET_MODEL_DUPLICATE_NAME",
                "Ya existe un modelo con el nombre '" + name + "' para la marca indicada"
        );
    }

    public static AssetModelException inUse(String id) {
        return new AssetModelException(
                HttpStatus.BAD_REQUEST,
                "ASSET_MODEL_IN_USE",
                "No se puede eliminar el modelo con id: " + id + " porque tiene activos asociados"
        );
    }
}
