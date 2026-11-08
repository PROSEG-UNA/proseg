package com.proseg.msvc_forms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FormRecordNotFoundException extends ResponseStatusException {

    public FormRecordNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
