package com.proseg.msvc_maintenance.exception;

import com.proseg.common.api.exception.BaseException;
import com.proseg.common.api.response.ApiErrorResponse;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleBase mapea BaseException a status y cuerpo ApiErrorResponse")
    void handleBase_cuandoBaseException_retornaStatusYCodigo() {
        BaseException ex = CompanyException.notFound();

        ResponseEntity<ApiErrorResponse> response = handler.handleBase(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getMessage()).isEqualTo(ex.getMessage());
        assertThat(body.getErrors()).containsExactly("COMPANY_NOT_FOUND");
    }

    @Test
    @DisplayName("handleIllegalArgument devuelve 400 con INVALID_ARGUMENT")
    void handleIllegalArgument_cuandoHayMensaje_retornaBadRequest() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("dato inválido"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getMessage()).isEqualTo("dato inválido");
        assertThat(body.getErrors()).containsExactly("INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("handleIllegalArgument sin mensaje usa texto por defecto")
    void handleIllegalArgument_cuandoMensajeNulo_usaDefault() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException((String) null));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Argumento inválido");
    }

    @Test
    @DisplayName("handleValidation agrega errores de campo al cuerpo")
    void handleValidation_cuandoHayFieldErrors_retornaListaErrores() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "es obligatorio"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Error de validación");
        assertThat(body.getErrors()).containsExactly("name: es obligatorio");
        assertThat(body.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("handleResponseStatusException respeta el status de la excepción")
    void handleResponseStatusException_cuando404_retornaNotFound() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "no existe");

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getMessage()).isEqualTo("no existe");
        assertThat(body.getErrors()).containsExactly("SPRING_ERROR");
    }

    @Test
    @DisplayName("handleFeign devuelve 502 con UPSTREAM_SERVICE_ERROR")
    void handleFeign_cuandoFallaCliente_retornaBadGateway() {
        FeignException feign = mock(FeignException.class);
        when(feign.status()).thenReturn(503);
        when(feign.getMessage()).thenReturn("upstream down");

        ResponseEntity<ApiErrorResponse> response = handler.handleFeign(feign);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(502);
        assertThat(body.getErrors()).containsExactly("UPSTREAM_SERVICE_ERROR");
        assertThat(body.getMessage()).contains("servicio interno");
    }

    @Test
    @DisplayName("handleGeneric con excepción no controlada devuelve 500")
    void handleGeneric_cuandoRuntimeException_retornaInternalServerError() {
        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getErrors()).containsExactly("SPRING_ERROR");
        assertThat(body.getMessage()).isEqualTo("Ocurrio un error inesperado al procesar la solicitud.");
    }

    @Test
    @DisplayName("handleGeneric con ResponseStatusException anidada respeta su status")
    void handleGeneric_cuandoResponseStatusException_retornaStatusDeLaExcepcion() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "conflicto");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("conflicto");
    }
}
