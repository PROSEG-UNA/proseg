package com.proseg.msvcinventory.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AssetComponentException extends BaseException {

    public AssetComponentException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AssetComponentException notFound(String id) {
        return new AssetComponentException(
                HttpStatus.NOT_FOUND,
                "ASSET_COMPONENT_NOT_FOUND",
                "Componente no encontrado con id: " + id
        );
    }
}

