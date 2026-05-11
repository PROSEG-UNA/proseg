package com.sssi.msvc_archive.config;

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
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
