package com.proseg.msvc_archive.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MinioPropertiesTest {

    @Test
    @DisplayName("getters y setters conservan valores de configuración")
    void propiedades_roundTrip() {
        MinioProperties properties = new MinioProperties();
        properties.setUrl("http://localhost:9000");
        properties.setAccessKey("user");
        properties.setSecretKey("secret");
        properties.setBucket("proseg");
        properties.setRegion("us-east-1");
        properties.setSecure(false);

        assertThat(properties.getUrl()).isEqualTo("http://localhost:9000");
        assertThat(properties.getAccessKey()).isEqualTo("user");
        assertThat(properties.getSecretKey()).isEqualTo("secret");
        assertThat(properties.getBucket()).isEqualTo("proseg");
        assertThat(properties.getRegion()).isEqualTo("us-east-1");
        assertThat(properties.isSecure()).isFalse();
    }
}
