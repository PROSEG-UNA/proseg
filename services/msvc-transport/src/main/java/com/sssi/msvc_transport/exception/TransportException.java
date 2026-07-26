package com.sssi.msvc_transport.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class TransportException extends BaseException {

    public TransportException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static TransportException notFound(String resource, String id) {
        return new TransportException(
                HttpStatus.NOT_FOUND,
                "TRANSPORT_RESOURCE_NOT_FOUND",
                resource + " no encontrado con id: " + id
        );
    }

    public static TransportException conflict(String code, String message) {
        return new TransportException(HttpStatus.CONFLICT, code, message);
    }

    public static TransportException badRequest(String code, String message) {
        return new TransportException(HttpStatus.BAD_REQUEST, code, message);
    }
}
