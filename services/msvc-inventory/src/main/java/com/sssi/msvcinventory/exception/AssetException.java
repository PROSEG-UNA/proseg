package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AssetException extends BaseException {

    public AssetException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AssetException notFound(String id) {
        return new AssetException(
                HttpStatus.NOT_FOUND,
                "ASSET_NOT_FOUND",
                "Activo no encontrado con id: " + id
        );
    }

    public static AssetException networkInterfaceRequired(String assetTypeName) {
        return new AssetException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "ASSET_NETWORK_INTERFACE_REQUIRED",
                "El tipo de activo '" + assetTypeName + "' requiere IP y MAC"
        );
    }
}
