package com.proseg.msvc_archive.exception;

import com.proseg.common.api.response.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleBase: mapea BaseException a ApiErrorResponse")
    void handleBase_mapeaArchiveException() {
        ArchiveException ex = ArchiveException.invalidObjectName();

        ResponseEntity<ApiErrorResponse> response = handler.handleBase(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsExactly("ARCHIVE_INVALID_OBJECT_NAME");
        assertThat(response.getBody().getMessage()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("handleValidation: agrega errores de campo")
    void handleValidation_devuelveBadRequest() throws Exception {
        MethodParameter parameter = new MethodParameter(Dummy.class.getDeclaredMethod("setName", String.class), 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Dummy(), "dummy");
        bindingResult.addError(new FieldError("dummy", "name", "no debe estar vacío"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Error de validacion");
        assertThat(response.getBody().getErrors()).containsExactly("name: no debe estar vacío");
    }

    @Test
    @DisplayName("handleResponseStatusException: usa status y reason de Spring")
    void handleResponseStatusException_mapeaNotFound() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "recurso ausente");

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsExactly("SPRING_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("recurso ausente");
    }

    @Test
    @DisplayName("handleGeneric: excepción genérica devuelve 500")
    void handleGeneric_excepcionSimple_internalServerError() {
        Exception ex = new Exception("fallo inesperado");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsExactly("SPRING_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("fallo inesperado");
    }

    @Test
    @DisplayName("handleGeneric: ResponseStatusException dentro del handler genérico")
    void handleGeneric_responseStatusException_mapeaStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "petición inválida");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("petición inválida");
    }

    @Test
    @DisplayName("handleGeneric: @ResponseStatus en la excepción")
    void handleGeneric_anotacionResponseStatus_usaReason() {
        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new ConflictException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Conflicto de negocio");
    }

    static class Dummy {
        @SuppressWarnings("unused")
        void setName(String name) {
        }
    }

    @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Conflicto de negocio")
    static class ConflictException extends RuntimeException {
    }
}
