package com.proseg.msvc_document_processor.client;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FeignExceptionTranslatorTest {

    @Test
    void translates_inventory_error_body_and_resolves_status_and_code() {
        var exception = feignException("{\"message\":\"No existe\",\"status\":404,\"errors\":[\"ASSET_NOT_FOUND\"]}", 500);

        DocumentProcessorException result = new InventoryFeignExceptionTranslator().translate(exception);

        assertThat(result.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getErrorCode()).isEqualTo("ASSET_NOT_FOUND");
        assertThat(result.getMessage()).isEqualTo("No existe");
    }

    @Test
    void translates_maintenance_error_using_fallback_status_and_default_code() {
        var exception = feignException("{\"message\":\"Temporal\"}", 503);

        DocumentProcessorException result = new MaintenanceFeignExceptionTranslator().translate(exception);

        assertThat(result.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(result.getErrorCode()).isEqualTo("MAINTENANCE_ERROR");
        assertThat(result.getMessage()).isEqualTo("Temporal");
    }

    @Test
    void falls_back_for_missing_body_malformed_body_and_missing_or_blank_message() {
        var translator = new InventoryFeignExceptionTranslator();
        assertThat(translator.translate(feignException(null, 500)).getErrorCode()).isEqualTo("INVENTORY_UNAVAILABLE");
        assertThat(translator.translate(feignException("not-json", 500)).getErrorCode()).isEqualTo("INVENTORY_UNAVAILABLE");
        assertThat(translator.translate(feignException("{\"message\":\"  \"}", 500)).getErrorCode())
                .isEqualTo("INVENTORY_UNAVAILABLE");
        assertThat(translator.translate(feignException("{\"message\":\"error\",\"status\":999}", 500)).getHttpStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void uses_default_error_code_when_errors_are_absent_empty_or_not_text() {
        var translator = new MaintenanceFeignExceptionTranslator();
        assertThat(translator.translate(feignException("{\"message\":\"x\",\"errors\":[]}", 400)).getErrorCode())
                .isEqualTo("MAINTENANCE_ERROR");
        assertThat(translator.translate(feignException("{\"message\":\"x\",\"errors\":[1]}", 400)).getErrorCode())
                .isEqualTo("MAINTENANCE_ERROR");
    }

    @Test
    void maintenance_translator_falls_back_for_missing_or_invalid_responses() {
        var translator = new MaintenanceFeignExceptionTranslator();
        assertThat(translator.translate(feignException(null, 503)).getErrorCode())
                .isEqualTo("MAINTENANCE_UNAVAILABLE");
        assertThat(translator.translate(feignException("not-json", 503)).getErrorCode())
                .isEqualTo("MAINTENANCE_UNAVAILABLE");
        assertThat(translator.translate(feignException("{\"message\":null}", 503)).getErrorCode())
                .isEqualTo("MAINTENANCE_UNAVAILABLE");
        assertThat(translator.translate(feignException(
                "{\"message\":\"x\",\"status\":999,\"errors\":[\"BAD\"]}", 503)).getHttpStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    private static FeignException feignException(String json, int status) {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(status);
        when(exception.responseBody()).thenReturn(json == null
                ? Optional.empty()
                : Optional.of(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8))));
        return exception;
    }
}
