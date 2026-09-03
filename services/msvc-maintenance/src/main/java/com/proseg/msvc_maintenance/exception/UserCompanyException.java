package com.proseg.msvc_maintenance.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserCompanyException extends BaseException {

    public UserCompanyException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static UserCompanyException notFound() {
        return new UserCompanyException(
                HttpStatus.NOT_FOUND,
                "USER_COMPANY_NOT_FOUND",
                "No encontramos la asignacion de empresa solicitada."
        );
    }

    public static UserCompanyException duplicateRelation() {
        return new UserCompanyException(
                HttpStatus.CONFLICT,
                "USER_COMPANY_DUPLICATE",
                "Este usuario ya tiene asignada esa empresa."
        );
    }
}

