package com.sssi.msvc_archive.service;

import com.sssi.msvc_archive.config.MinioProperties;
import com.sssi.msvc_archive.dto.ArchiveUploadInitResponseDto;
import com.sssi.msvc_archive.dto.ArchiveUploadPartResponseDto;
import com.sssi.msvc_archive.dto.ArchiveUploadResponseDto;
import com.sssi.msvc_archive.exception.ArchiveException;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ArchiveService {

    private static final int PRESIGNED_URL_MINUTES = 10;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public ArchiveService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public ArchiveUploadInitResponseDto initiateMultipartUpload(
            String filename,
            String objectName,
            String folder,
            String contentType
    ) {
        String uploadId = UUID.randomUUID().toString();
        String resolvedObjectName = resolveObjectName(filename, objectName, folder);

        ArchiveUploadInitResponseDto response = new ArchiveUploadInitResponseDto();
        response.setUploadId(uploadId);
        response.setObjectName(resolvedObjectName);
        response.setContentType(resolveContentType(contentType));

        return response;
    }

    public ArchiveUploadPartResponseDto uploadPart(
            String uploadId,
            String objectName,
            int partNumber,
            MultipartFile chunk
    ) {
        try {
            String partObjectName = getPartObjectName(uploadId, objectName, partNumber);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(partObjectName)
                            .stream(chunk.getInputStream(), chunk.getSize(), -1)
                            .contentType(resolveContentType(chunk.getContentType()))
                            .build()
            );

            ArchiveUploadPartResponseDto response = new ArchiveUploadPartResponseDto();
            response.setUploadId(uploadId);
            response.setObjectName(objectName);
            response.setPartNumber(partNumber);
            response.setSize(chunk.getSize());

            return response;
        } catch (Exception exception) {
            throw ArchiveException.uploadPartError();
        }
    }

    public ArchiveUploadResponseDto completeMultipartUpload(
            String uploadId,
            String objectName,
            long totalSize
    ) {
        try {
            List<ComposeSource> sources = new ArrayList<>();
            int partNumber = 1;

            while (true) {
                String partObjectName = getPartObjectName(uploadId, objectName, partNumber);

                try {
                    minioClient.statObject(
                            StatObjectArgs.builder()
                                    .bucket(minioProperties.getBucket())
                                    .object(partObjectName)
                                    .build()
                    );
                } catch (Exception exception) {
                    break;
                }

                sources.add(
                        ComposeSource.builder()
                                .bucket(minioProperties.getBucket())
                                .object(partObjectName)
                                .build()
                );

                partNumber++;
            }

            if (sources.isEmpty()) {
                throw ArchiveException.multipartUploadIncomplete();
            }

            if (sources.size() == 1) {
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .object(objectName)
                                .source(
                                        CopySource.builder()
                                                .bucket(minioProperties.getBucket())
                                                .object(getPartObjectName(uploadId, objectName, 1))
                                                .build()
                                )
                                .build()
                );
            } else {
                minioClient.composeObject(
                        ComposeObjectArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .object(objectName)
                                .sources(sources)
                                .build()
                );
            }

            for (int index = 1; index < partNumber; index++) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .object(getPartObjectName(uploadId, objectName, index))
                                .build()
                );
            }

            ArchiveUploadResponseDto response = new ArchiveUploadResponseDto();
            response.setObjectName(objectName);
            response.setBucket(minioProperties.getBucket());
            response.setSize(totalSize);

            return response;
        } catch (ArchiveException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ArchiveException.completeUploadError();
        }
    }

    public ArchiveDownload download(String objectName) {
        try {
            StatObjectResponse statObjectResponse = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .build()
            );

            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .build()
            );

            return new ArchiveDownload(
                    objectName,
                    resolveContentType(statObjectResponse.contentType()),
                    statObjectResponse.size(),
                    stream
            );
        } catch (Exception exception) {
            throw ArchiveException.downloadError();
        }
    }

    public String getPresignedGetUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .expiry(PRESIGNED_URL_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception exception) {
            throw ArchiveException.presignedUrlError();
        }
    }

    private String resolveObjectName(String filename, String objectName, String folder) {
        if (objectName != null && !objectName.isBlank()) {
            return objectName;
        }

        String resolvedFolder = folder == null || folder.isBlank()
                ? ""
                : folder.replace("\\", "/").replaceAll("/+$", "") + "/";

        return resolvedFolder + filename;
    }

    private String getPartObjectName(String uploadId, String objectName, int partNumber) {
        return ".multipart/" + uploadId + "/" + objectName + ".part" + partNumber;
    }

    private String resolveContentType(String contentType) {
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
    }
}