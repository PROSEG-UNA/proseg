package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AssetTypeException extends BaseException {

    public AssetTypeException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AssetTypeException notFound(String id) {
        return new AssetTypeException(
                HttpStatus.NOT_FOUND,
                "ASSET_TYPE_NOT_FOUND",
                "Tipo de activo no encontrado con id: " + id
        );
    }

    public static AssetTypeException duplicateName(String name) {
        return new AssetTypeException(
                HttpStatus.CONFLICT,
                "ASSET_TYPE_DUPLICATE_NAME",
                "Ya existe un tipo de activo con el nombre: " + name
        );
    }

    public static AssetTypeException inUse(String id) {
        return new AssetTypeException(
                HttpStatus.BAD_REQUEST,
                "ASSET_TYPE_IN_USE",
                "No se puede eliminar el tipo de activo con id: " + id + " porque tiene activos o modelos asociados"
        );
    }
}
