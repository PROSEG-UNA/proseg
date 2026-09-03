package com.proseg.msvcinventory.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AssetArchiveException extends BaseException {

    public AssetArchiveException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AssetArchiveException notFound(String id) {
        return new AssetArchiveException(
                HttpStatus.NOT_FOUND,
                "ASSET_ARCHIVE_NOT_FOUND",
                "Archivo no encontrado con id: " + id
        );
    }
}
