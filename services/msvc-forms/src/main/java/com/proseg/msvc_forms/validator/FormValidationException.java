package com.proseg.msvc_forms.validator;

public class FormValidationException extends RuntimeException {

    private final String errorCode;

    public FormValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
    }

    public FormValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
