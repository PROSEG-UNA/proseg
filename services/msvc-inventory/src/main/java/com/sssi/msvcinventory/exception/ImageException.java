package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ImageException extends BaseException {

    public ImageException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ImageException notFound(String id) {
        return new ImageException(
                HttpStatus.NOT_FOUND,
                "ASSET_IMAGE_NOT_FOUND",
                "Imagen no encontrada con id: " + id
        );
    }
}
