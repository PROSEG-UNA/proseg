package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AssetImageException extends BaseException {

    public AssetImageException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AssetImageException notFound(String id) {
        return new AssetImageException(
                HttpStatus.NOT_FOUND,
                "ASSET_IMAGE_NOT_FOUND",
                "Imagen no encontrada con id: " + id
        );
    }
}
