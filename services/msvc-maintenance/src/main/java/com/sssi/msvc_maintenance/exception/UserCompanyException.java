package com.sssi.msvc_maintenance.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserCompanyException extends BaseException {

    public UserCompanyException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static UserCompanyException notFound(String id) {
        return new UserCompanyException(
                HttpStatus.NOT_FOUND,
                "USER_COMPANY_NOT_FOUND",
                "Relación usuario-compañía no encontrada con id: " + id
        );
    }

    public static UserCompanyException duplicateRelation(String keycloakUserId, String companyId) {
        return new UserCompanyException(
                HttpStatus.CONFLICT,
                "USER_COMPANY_DUPLICATE",
                "La relación ya existe para usuario '" + keycloakUserId + "' y compañía '" + companyId + "'"
        );
    }
}

