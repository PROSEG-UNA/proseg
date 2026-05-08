package com.sssi.msvc_archive.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ArchiveException extends BaseException {

    public ArchiveException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ArchiveException invalidFile() {
        return new ArchiveException(
                HttpStatus.BAD_REQUEST,
                "ARCHIVE_INVALID_FILE",
                "El archivo es invalido"
        );
    }

    public static ArchiveException notFound(String objectName) {
        return new ArchiveException(
                HttpStatus.NOT_FOUND,
                "ARCHIVE_NOT_FOUND",
                "Archivo no encontrado: " + objectName
        );
    }

    public static ArchiveException configurationError(String message) {
        return new ArchiveException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ARCHIVE_CONFIG_ERROR",
                message
        );
    }

    public static ArchiveException minioError(String message) {
        return new ArchiveException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ARCHIVE_MINIO_ERROR",
                message
        );
    }
}
