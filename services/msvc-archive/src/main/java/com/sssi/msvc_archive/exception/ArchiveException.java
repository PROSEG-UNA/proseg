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
                "El archivo es inválido"
        );
    }

    public static ArchiveException notFound(String objectName) {
        return new ArchiveException(
                HttpStatus.NOT_FOUND,
                "ARCHIVE_NOT_FOUND",
                "Archivo no encontrado: " + objectName
        );
    }

    public static ArchiveException uploadPartError() {
        return new ArchiveException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ARCHIVE_UPLOAD_PART_ERROR",
                "No fue posible subir la parte del archivo"
        );
    }

    public static ArchiveException multipartUploadIncomplete() {
        return new ArchiveException(
                HttpStatus.BAD_REQUEST,
                "ARCHIVE_MULTIPART_INCOMPLETE",
                "No existen partes para completar la carga"
        );
    }

    public static ArchiveException completeUploadError() {
        return new ArchiveException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ARCHIVE_COMPLETE_UPLOAD_ERROR",
                "No fue posible completar la carga por partes"
        );
    }

    public static ArchiveException downloadError() {
        return new ArchiveException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ARCHIVE_DOWNLOAD_ERROR",
                "No fue posible descargar el archivo"
        );
    }

    public static ArchiveException presignedUrlError() {
        return new ArchiveException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ARCHIVE_PRESIGNED_URL_ERROR",
                "No fue posible generar la URL firmada"
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