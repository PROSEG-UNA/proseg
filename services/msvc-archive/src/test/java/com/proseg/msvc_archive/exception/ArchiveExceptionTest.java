package com.proseg.msvc_archive.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveExceptionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("factoryCases")
    @DisplayName("factorías estáticas: HttpStatus y errorCode esperados")
    void factory_devuelveStatusYCodigo(String label, Supplier<ArchiveException> factory, HttpStatus status, String errorCode) {
        ArchiveException ex = factory.get();

        assertThat(ex.getHttpStatus()).isEqualTo(status);
        assertThat(ex.getErrorCode()).isEqualTo(errorCode);
        assertThat(ex.getMessage()).isNotBlank();
    }

    static Stream<Arguments> factoryCases() {
        return Stream.of(
                Arguments.of("invalidFile", (Supplier<ArchiveException>) ArchiveException::invalidFile,
                        HttpStatus.BAD_REQUEST, "ARCHIVE_INVALID_FILE"),
                Arguments.of("invalidObjectName", (Supplier<ArchiveException>) ArchiveException::invalidObjectName,
                        HttpStatus.BAD_REQUEST, "ARCHIVE_INVALID_OBJECT_NAME"),
                Arguments.of("invalidFolder", (Supplier<ArchiveException>) ArchiveException::invalidFolder,
                        HttpStatus.BAD_REQUEST, "ARCHIVE_INVALID_FOLDER"),
                Arguments.of("invalidUploadId", (Supplier<ArchiveException>) ArchiveException::invalidUploadId,
                        HttpStatus.BAD_REQUEST, "ARCHIVE_INVALID_UPLOAD_ID"),
                Arguments.of("invalidPartNumber", (Supplier<ArchiveException>) ArchiveException::invalidPartNumber,
                        HttpStatus.BAD_REQUEST, "ARCHIVE_INVALID_PART_NUMBER"),
                Arguments.of("notFound", (Supplier<ArchiveException>) () -> ArchiveException.notFound("obj.bin"),
                        HttpStatus.NOT_FOUND, "ARCHIVE_NOT_FOUND"),
                Arguments.of("uploadPartError", (Supplier<ArchiveException>) ArchiveException::uploadPartError,
                        HttpStatus.INTERNAL_SERVER_ERROR, "ARCHIVE_UPLOAD_PART_ERROR"),
                Arguments.of("multipartUploadIncomplete", (Supplier<ArchiveException>) ArchiveException::multipartUploadIncomplete,
                        HttpStatus.BAD_REQUEST, "ARCHIVE_MULTIPART_INCOMPLETE"),
                Arguments.of("completeUploadError", (Supplier<ArchiveException>) ArchiveException::completeUploadError,
                        HttpStatus.INTERNAL_SERVER_ERROR, "ARCHIVE_COMPLETE_UPLOAD_ERROR"),
                Arguments.of("downloadError", (Supplier<ArchiveException>) ArchiveException::downloadError,
                        HttpStatus.INTERNAL_SERVER_ERROR, "ARCHIVE_DOWNLOAD_ERROR"),
                Arguments.of("presignedUrlError", (Supplier<ArchiveException>) ArchiveException::presignedUrlError,
                        HttpStatus.INTERNAL_SERVER_ERROR, "ARCHIVE_PRESIGNED_URL_ERROR"),
                Arguments.of("configurationError", (Supplier<ArchiveException>) () -> ArchiveException.configurationError("fallo config"),
                        HttpStatus.INTERNAL_SERVER_ERROR, "ARCHIVE_CONFIG_ERROR"),
                Arguments.of("minioError", (Supplier<ArchiveException>) () -> ArchiveException.minioError("fallo minio"),
                        HttpStatus.INTERNAL_SERVER_ERROR, "ARCHIVE_MINIO_ERROR")
        );
    }
}
