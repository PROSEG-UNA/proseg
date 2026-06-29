package com.sssi.msvc_archive.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@SuppressWarnings("all")
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        String endpoint = Objects.requireNonNullElse(properties.getUrl(), "").trim();
        if (endpoint.isEmpty()) {
            throw new IllegalStateException("MinIO endpoint no configurado");
        }
        //noinspection ConstantConditions
        if (!endpoint.startsWith("http")) {
            endpoint = (properties.isSecure() ? "https://" : "http://") + endpoint;
        }
        log.info("MinIO endpoint resolved: {} (secure={})", endpoint, properties.isSecure());

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(properties.getAccessKey(), properties.getSecretKey());

        String region = properties.getRegion();
        if (region != null && !region.isBlank()) {
            builder.region(region.trim());
        }

        MinioClient client = builder.build();

        ensureBucketExists(client, properties);

        return client;
    }

    private void ensureBucketExists(MinioClient client, MinioProperties properties) {
        String bucket = properties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            log.warn("MinIO bucket no configurado; se omite la verificación de existencia");
            return;
        }

        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (exists) {
                log.info("Bucket MinIO disponible: {}", bucket);
                return;
            }

            MakeBucketArgs.Builder makeBucket = MakeBucketArgs.builder().bucket(bucket);
            String region = properties.getRegion();
            if (region != null && !region.isBlank()) {
                makeBucket.region(region.trim());
            }
            client.makeBucket(makeBucket.build());
            log.info("Bucket MinIO creado: {}", bucket);
        } catch (Exception exception) {
            // No tumbamos el arranque: dejamos el motivo explícito en logs para diagnóstico.
            log.error("No fue posible verificar/crear el bucket MinIO '{}'. "
                    + "Créalo manualmente o concede permisos al usuario configurado; "
                    + "las subidas fallarán con ARCHIVE_UPLOAD_PART_ERROR hasta resolverlo.",
                    bucket, exception);
        }
    }
}
