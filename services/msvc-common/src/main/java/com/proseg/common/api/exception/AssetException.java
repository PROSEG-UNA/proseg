package com.proseg.common.api.exception;

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
}
