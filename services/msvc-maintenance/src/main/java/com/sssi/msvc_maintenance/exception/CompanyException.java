package com.sssi.msvc_maintenance.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CompanyException extends BaseException {

    public CompanyException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static CompanyException notFound(String id) {
        return new CompanyException(
                HttpStatus.NOT_FOUND,
                "COMPANY_NOT_FOUND",
                "Compañía no encontrada con id: " + id
        );
    }

    public static CompanyException duplicateName(String name) {
        return new CompanyException(
                HttpStatus.CONFLICT,
                "COMPANY_DUPLICATE_NAME",
                "Ya existe una compañía con el nombre: " + name
        );
    }

    public static CompanyException duplicateLegalId(String legalId) {
        return new CompanyException(
                HttpStatus.CONFLICT,
                "COMPANY_DUPLICATE_LEGAL_ID",
                "Ya existe una compañía con la cédula jurídica: " + legalId
        );
    }

    public static CompanyException inUse(String name) {
        return new CompanyException(
                HttpStatus.BAD_REQUEST,
                "COMPANY_IN_USE",
                "No se puede eliminar la compañía '" + name + "' porque tiene registros relacionados"
        );
    }
}

