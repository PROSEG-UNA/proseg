package com.proseg.msvc_document_processor.exception;

import com.proseg.common.api.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void maps_domain_exception_to_original_status_and_error_code() {
        var exception = DocumentProcessorException.unsupportedDocumentType("unknown");

        ResponseEntity<ApiErrorResponse> result = handler.handleBase(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).satisfies(body -> {
            assertThat(body.isSuccess()).isFalse();
            assertThat(body.getErrors()).containsExactly("UNSUPPORTED_DOCUMENT_TYPE");
            assertThat(body.getMessage()).contains("unknown");
        });
    }

    @Test
    void maps_upload_size_limit_to_payload_too_large() {
        ResponseEntity<ApiErrorResponse> result = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(1));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(result.getBody().getErrors()).containsExactly("MAX_UPLOAD_SIZE_EXCEEDED");
    }

    @Test
    void maps_validation_field_errors_to_api_response() {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "name", "es obligatorio"));
        var exception = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<ApiErrorResponse> result = handler.handleValidation(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().getMessage()).isEqualTo("Error de validación");
        assertThat(result.getBody().getErrors()).containsExactly("name: es obligatorio");
    }

    @Test
    void maps_unexpected_error_without_exposing_internal_message() {
        ResponseEntity<ApiErrorResponse> result = handler.handleGeneric(new IllegalStateException("private detail"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().getErrors()).containsExactly("INTERNAL_ERROR");
        assertThat(result.getBody().getMessage()).doesNotContain("private detail");
    }
}
