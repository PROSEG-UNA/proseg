package com.proseg.msvc_archive.service;

import com.proseg.msvc_archive.config.MinioProperties;
import com.proseg.common.api.response.ArchiveUploadInitResponseDto;
import com.proseg.common.api.response.ArchiveUploadPartResponseDto;
import com.proseg.common.api.response.ArchiveUploadResponseDto;
import com.proseg.msvc_archive.exception.ArchiveException;
import io.minio.ComposeObjectArgs;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String UPLOAD_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String OBJECT_NAME = "docs/archivo.pdf";

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties minioProperties;

    @InjectMocks
    private ArchiveService archiveService;

    @BeforeEach
    void setUp() {
        lenient().when(minioProperties.getBucket()).thenReturn(BUCKET);
    }

    @Test
    @DisplayName("initiateMultipartUpload: con objectName dado lo normaliza y devuelve")
    void initiateMultipartUpload_conObjectNameDado_normalizaYDevuelve() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "ignored.png",
                "  docs\\archivo.pdf  ",
                "carpeta",
                "application/pdf"
        );

        assertThat(response.getUploadId()).isNotBlank();
        UUID.fromString(response.getUploadId());
        assertThat(response.getObjectName()).isEqualTo("docs/archivo.pdf");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("initiateMultipartUpload: sin objectName genera folder/UUID y extensión en minúscula")
    void initiateMultipartUpload_sinObjectName_generaRutaConExtensionMinuscula() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "FOTO.PNG",
                null,
                "fotos",
                "image/png"
        );

        assertThat(response.getObjectName()).matches("fotos/[0-9a-f\\-]+\\.png");
        assertThat(response.getContentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("initiateMultipartUpload: extensión inválida no añade sufijo")
    void initiateMultipartUpload_extensionInvalida_sinExtension() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "archivo.mal ext",
                "",
                "data",
                "application/octet-stream"
        );

        assertThat(response.getObjectName()).matches("data/[0-9a-f\\-]+$");
        assertThat(response.getObjectName()).doesNotContain(".");
    }

    @Test
    @DisplayName("initiateMultipartUpload: sin carpeta genera solo UUID y extensión")
    void initiateMultipartUpload_sinCarpeta_soloUuidYExtension() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "doc.txt",
                null,
                null,
                "text/plain"
        );

        assertThat(response.getObjectName()).matches("[0-9a-f\\-]+\\.txt");
        assertThat(response.getObjectName()).doesNotStartWith("/");
    }

    @Test
    @DisplayName("initiateMultipartUpload: contentType null usa application/octet-stream")
    void initiateMultipartUpload_contentTypeNull_usaOctetStream() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "a.bin",
                "obj.bin",
                null,
                null
        );

        assertThat(response.getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("initiateMultipartUpload: objectName con .. lanza invalidObjectName")
    void initiateMultipartUpload_objectNameConPuntos_invalidObjectName() throws Exception {
        assertThatThrownBy(() -> archiveService.initiateMultipartUpload(
                "x.png",
                "../secreto",
                null,
                null
        ))
                .isInstanceOfSatisfying(ArchiveException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_OBJECT_NAME");
                });

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("initiateMultipartUpload: carpeta inválida lanza invalidFolder")
    void initiateMultipartUpload_carpetaInvalida_invalidFolder() throws Exception {
        assertThatThrownBy(() -> archiveService.initiateMultipartUpload(
                "x.png",
                null,
                "../mal",
                null
        ))
                .isInstanceOfSatisfying(ArchiveException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_FOLDER");
                });
    }

    @Test
    @DisplayName("initiateMultipartUpload: objectName que empieza con / lanza invalidObjectName")
    void initiateMultipartUpload_objectNameBarraInicial_invalidObjectName() throws Exception {
        assertThatThrownBy(() -> archiveService.initiateMultipartUpload("a.txt", "/docs/a.txt", null, null))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_OBJECT_NAME"));
    }

    @Test
    @DisplayName("initiateMultipartUpload: objectName que termina con / lanza invalidObjectName")
    void initiateMultipartUpload_objectNameBarraFinal_invalidObjectName() throws Exception {
        assertThatThrownBy(() -> archiveService.initiateMultipartUpload("a.txt", "docs/carpeta/", null, null))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_OBJECT_NAME"));
    }

    @Test
    @DisplayName("initiateMultipartUpload: objectName con caracteres no permitidos lanza invalidObjectName")
    void initiateMultipartUpload_objectNameConEspacio_invalidObjectName() throws Exception {
        assertThatThrownBy(() -> archiveService.initiateMultipartUpload("a.txt", "a b.txt", null, null))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_OBJECT_NAME"));
    }

    @Test
    @DisplayName("initiateMultipartUpload: objectName null genera nombre con UUID")
    void initiateMultipartUpload_objectNameNull_generaUuid() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "file.dat",
                null,
                "uploads",
                null
        );

        assertThat(response.getObjectName()).matches("uploads/[0-9a-f\\-]+\\.dat");
        assertThat(response.getUploadId()).isNotBlank();
    }

    @Test
    @DisplayName("initiateMultipartUpload: objectName en blanco genera nombre con UUID")
    void initiateMultipartUpload_objectNameEnBlanco_generaUuid() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "file.dat",
                "   ",
                "uploads",
                null
        );

        assertThat(response.getObjectName()).matches("uploads/[0-9a-f\\-]+\\.dat");
    }

    @Test
    @DisplayName("initiateMultipartUpload: folder que empieza con / lanza invalidFolder")
    void initiateMultipartUpload_folderBarraInicial_invalidFolder() throws Exception {
        assertThatThrownBy(() -> archiveService.initiateMultipartUpload("x.bin", null, "/docs", null))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_FOLDER"));
    }

    @Test
    @DisplayName("initiateMultipartUpload: folder con .. lanza invalidFolder")
    void initiateMultipartUpload_folderConPuntos_invalidFolder() throws Exception {
        assertThatThrownBy(() -> archiveService.initiateMultipartUpload("x.bin", null, "docs/../otro", null))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_FOLDER"));
    }

    @Test
    @DisplayName("initiateMultipartUpload: folder con barra final genera docs/<uuid>")
    void initiateMultipartUpload_folderConBarraFinal_generaRutaDocs() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "img.jpg",
                null,
                "docs/",
                "image/jpeg"
        );

        assertThat(response.getObjectName()).matches("docs/[0-9a-f\\-]+\\.jpg");
    }

    @Test
    @DisplayName("initiateMultipartUpload: filename sin punto no añade extensión")
    void initiateMultipartUpload_filenameSinPunto_sinExtension() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "readme",
                null,
                "textos",
                null
        );

        assertThat(response.getObjectName()).matches("textos/[0-9a-f\\-]+$");
        assertThat(response.getObjectName()).doesNotContain(".");
    }

    @Test
    @DisplayName("initiateMultipartUpload: filename null no añade extensión")
    void initiateMultipartUpload_filenameNull_sinExtension() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                null,
                null,
                "raw",
                null
        );

        assertThat(response.getObjectName()).matches("raw/[0-9a-f\\-]+$");
        assertThat(response.getObjectName()).doesNotContain(".");
    }

    @Test
    @DisplayName("initiateMultipartUpload: objectName con barras invertidas se normaliza")
    void initiateMultipartUpload_objectNameConBackslash_normalizaSlash() throws Exception {
        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                "n/a",
                "a\\b.txt",
                null,
                null
        );

        assertThat(response.getObjectName()).isEqualTo("a/b.txt");
    }

    @Test
    @DisplayName("uploadPart: camino feliz sube parte y responde metadatos")
    void uploadPart_caminoFeliz_subeParte() throws Exception {
        MockMultipartFile chunk = new MockMultipartFile(
                "chunk",
                "part.bin",
                "application/octet-stream",
                new byte[]{1, 2, 3}
        );

        ArchiveUploadPartResponseDto response = archiveService.uploadPart(
                UPLOAD_ID,
                OBJECT_NAME,
                1,
                chunk
        );

        assertThat(response.getUploadId()).isEqualTo(UPLOAD_ID);
        assertThat(response.getObjectName()).isEqualTo(OBJECT_NAME);
        assertThat(response.getPartNumber()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(3L);

        ArgumentCaptor<PutObjectArgs> putCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(putCaptor.capture());
        assertThat(putCaptor.getValue().object())
                .isEqualTo(".multipart/" + UPLOAD_ID + "/" + OBJECT_NAME + ".part1");
        assertThat(putCaptor.getValue().bucket()).isEqualTo(BUCKET);
    }

    @Test
    @DisplayName("uploadPart: uploadId null lanza invalidUploadId sin error genérico")
    void uploadPart_uploadIdNull_invalidUploadId() throws Exception {
        MultipartFile chunk = mock(MultipartFile.class);

        assertThatThrownBy(() -> archiveService.uploadPart(null, OBJECT_NAME, 1, chunk))
                .isInstanceOfSatisfying(ArchiveException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_UPLOAD_ID");
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadPart: uploadId no UUID lanza invalidUploadId")
    void uploadPart_uploadIdNoUuid_invalidUploadId() throws Exception {
        MultipartFile chunk = mock(MultipartFile.class);

        assertThatThrownBy(() -> archiveService.uploadPart("no-es-uuid", OBJECT_NAME, 1, chunk))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_UPLOAD_ID"));

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadPart: parte 0 lanza invalidPartNumber")
    void uploadPart_parteCero_invalidPartNumber() throws Exception {
        MultipartFile chunk = mock(MultipartFile.class);

        assertThatThrownBy(() -> archiveService.uploadPart(UPLOAD_ID, OBJECT_NAME, 0, chunk))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_PART_NUMBER"));

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadPart: parte -1 lanza invalidPartNumber")
    void uploadPart_parteNegativa_invalidPartNumber() throws Exception {
        MultipartFile chunk = mock(MultipartFile.class);

        assertThatThrownBy(() -> archiveService.uploadPart(UPLOAD_ID, OBJECT_NAME, -1, chunk))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_PART_NUMBER"));
    }

    @Test
    @DisplayName("uploadPart: putObject falla lanza uploadPartError")
    void uploadPart_putObjectFalla_uploadPartError() throws Exception {
        MockMultipartFile chunk = new MockMultipartFile("c", "p", "text/plain", new byte[]{9});
        doThrow(new RuntimeException("minio down")).when(minioClient).putObject(any(PutObjectArgs.class));

        assertThatThrownBy(() -> archiveService.uploadPart(UPLOAD_ID, OBJECT_NAME, 1, chunk))
                .isInstanceOfSatisfying(ArchiveException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_UPLOAD_PART_ERROR");
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    @Test
    @DisplayName("uploadPart: IOException al leer chunk lanza uploadPartError")
    void uploadPart_inputStreamFalla_uploadPartError() throws Exception {
        MultipartFile chunk = mock(MultipartFile.class);
        when(chunk.getInputStream()).thenThrow(new IOException("read fail"));

        assertThatThrownBy(() -> archiveService.uploadPart(UPLOAD_ID, OBJECT_NAME, 1, chunk))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_UPLOAD_PART_ERROR"));
    }

    @Test
    @DisplayName("uploadPart: objectName inválido no se convierte en uploadPartError")
    void uploadPart_objectNameInvalido_mantieneCodigoValidacion() throws Exception {
        MultipartFile chunk = mock(MultipartFile.class);

        assertThatThrownBy(() -> archiveService.uploadPart(UPLOAD_ID, "../evil", 1, chunk))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_OBJECT_NAME"));

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("completeMultipartUpload: sin partes lanza multipartUploadIncomplete")
    void completeMultipartUpload_sinPartes_multipartUploadIncomplete() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("missing"));

        assertThatThrownBy(() -> archiveService.completeMultipartUpload(UPLOAD_ID, OBJECT_NAME, 0L))
                .isInstanceOfSatisfying(ArchiveException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_MULTIPART_INCOMPLETE");
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(minioClient, never()).copyObject(any(CopyObjectArgs.class));
        verify(minioClient, never()).composeObject(any(ComposeObjectArgs.class));
    }

    @Test
    @DisplayName("completeMultipartUpload: una parte usa copyObject")
    void completeMultipartUpload_unaParte_copyObject() throws Exception {
        StatObjectResponse stat = mockStat(100L, "application/pdf");
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenReturn(stat)
                .thenThrow(new RuntimeException("no more parts"));

        ArchiveUploadResponseDto response = archiveService.completeMultipartUpload(
                UPLOAD_ID,
                OBJECT_NAME,
                100L
        );

        assertThat(response.getObjectName()).isEqualTo(OBJECT_NAME);
        assertThat(response.getBucket()).isEqualTo(BUCKET);
        assertThat(response.getSize()).isEqualTo(100L);

        ArgumentCaptor<CopyObjectArgs> copyCaptor = ArgumentCaptor.forClass(CopyObjectArgs.class);
        verify(minioClient).copyObject(copyCaptor.capture());
        assertThat(copyCaptor.getValue().object()).isEqualTo(OBJECT_NAME);
        assertThat(copyCaptor.getValue().bucket()).isEqualTo(BUCKET);

        verify(minioClient, never()).composeObject(any(ComposeObjectArgs.class));
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("completeMultipartUpload: varias partes usa composeObject y elimina cada parte")
    void completeMultipartUpload_variasPartes_composeYRemove() throws Exception {
        StatObjectResponse stat = mockStat(50L, "application/octet-stream");
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenReturn(stat)
                .thenReturn(stat)
                .thenThrow(new RuntimeException("no part 3"));

        ArchiveUploadResponseDto response = archiveService.completeMultipartUpload(
                UPLOAD_ID,
                OBJECT_NAME,
                100L
        );

        assertThat(response.getObjectName()).isEqualTo(OBJECT_NAME);

        ArgumentCaptor<ComposeObjectArgs> composeCaptor = ArgumentCaptor.forClass(ComposeObjectArgs.class);
        verify(minioClient).composeObject(composeCaptor.capture());
        assertThat(composeCaptor.getValue().object()).isEqualTo(OBJECT_NAME);
        assertThat(composeCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(composeCaptor.getValue().sources()).hasSize(2);

        verify(minioClient, never()).copyObject(any(CopyObjectArgs.class));
        verify(minioClient, times(2)).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("completeMultipartUpload: composeObject falla lanza completeUploadError")
    void completeMultipartUpload_composeFalla_completeUploadError() throws Exception {
        StatObjectResponse stat = mockStat(10L, "application/octet-stream");
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenReturn(stat)
                .thenReturn(stat)
                .thenThrow(new RuntimeException("no part 3"));
        doThrow(new RuntimeException("compose fail")).when(minioClient).composeObject(any(ComposeObjectArgs.class));

        assertThatThrownBy(() -> archiveService.completeMultipartUpload(UPLOAD_ID, OBJECT_NAME, 20L))
                .isInstanceOfSatisfying(ArchiveException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_COMPLETE_UPLOAD_ERROR");
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    @Test
    @DisplayName("completeMultipartUpload: uploadId inválido lanza invalidUploadId")
    void completeMultipartUpload_uploadIdInvalido_invalidUploadId() throws Exception {
        assertThatThrownBy(() -> archiveService.completeMultipartUpload("bad-id", OBJECT_NAME, 1L))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_UPLOAD_ID"));

        verify(minioClient, never()).statObject(any(StatObjectArgs.class));
    }

    @Test
    @DisplayName("download: camino feliz devuelve ArchiveDownload")
    void download_caminoFeliz_devuelveArchiveDownload() throws Exception {
        StatObjectResponse stat = mockStat(2048L, "image/jpeg");
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);
        GetObjectResponse body = mock(GetObjectResponse.class);
        when(minioClient.getObject(any())).thenReturn(body);

        ArchiveDownload download = archiveService.download(OBJECT_NAME);

        assertThat(download.objectName()).isEqualTo(OBJECT_NAME);
        assertThat(download.contentType()).isEqualTo("image/jpeg");
        assertThat(download.size()).isEqualTo(2048L);
        assertThat(download.stream()).isSameAs(body);
    }

    @Test
    @DisplayName("download: statObject falla lanza downloadError")
    void download_statObjectFalla_downloadError() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> archiveService.download(OBJECT_NAME))
                .isInstanceOfSatisfying(ArchiveException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_DOWNLOAD_ERROR");
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });

        verify(minioClient, never()).getObject(any());
    }

    @Test
    @DisplayName("download: nombre inválido lanza invalidObjectName sin downloadError")
    void download_nombreInvalido_invalidObjectName() throws Exception {
        assertThatThrownBy(() -> archiveService.download("..//hack"))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_OBJECT_NAME"));

        verify(minioClient, never()).statObject(any(StatObjectArgs.class));
    }

    @Test
    @DisplayName("getPresignedGetUrl: camino feliz GET con expiración 10 minutos")
    void getPresignedGetUrl_caminoFeliz_getConExpiry() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio.example/presigned");

        String url = archiveService.getPresignedGetUrl(OBJECT_NAME);

        assertThat(url).isEqualTo("https://minio.example/presigned");

        ArgumentCaptor<GetPresignedObjectUrlArgs> captor = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(captor.capture());
        GetPresignedObjectUrlArgs args = captor.getValue();
        assertThat(args.method()).isEqualTo(Method.GET);
        assertThat(args.bucket()).isEqualTo(BUCKET);
        assertThat(args.object()).isEqualTo(OBJECT_NAME);
        assertThat(args.expiry()).isEqualTo((int) TimeUnit.MINUTES.toSeconds(10));
    }

    @Test
    @DisplayName("getPresignedGetUrl: falla MinIO lanza presignedUrlError")
    void getPresignedGetUrl_fallaMinio_presignedUrlError() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("sign fail"));

        assertThatThrownBy(() -> archiveService.getPresignedGetUrl(OBJECT_NAME))
                .isInstanceOfSatisfying(ArchiveException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_PRESIGNED_URL_ERROR");
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    @Test
    @DisplayName("getPresignedGetUrl: objectName inválido mantiene código de validación")
    void getPresignedGetUrl_objectNameInvalido_noPresignedUrlError() throws Exception {
        assertThatThrownBy(() -> archiveService.getPresignedGetUrl("/leading"))
                .isInstanceOfSatisfying(ArchiveException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("ARCHIVE_INVALID_OBJECT_NAME"));

        verify(minioClient, never()).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    private static StatObjectResponse mockStat(long size, String contentType) {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        lenient().when(stat.size()).thenReturn(size);
        lenient().when(stat.contentType()).thenReturn(contentType);
        return stat;
    }
}
