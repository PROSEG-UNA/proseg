package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class EmployeeException extends BaseException {

    public EmployeeException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static EmployeeException notFound(String id) {
        return new EmployeeException(
                HttpStatus.NOT_FOUND,
                "EMPLOYEE_NOT_FOUND",
                "Funcionario no encontrado con id: " + id
        );
    }

    public static EmployeeException duplicateName(String name) {
        return new EmployeeException(
                HttpStatus.CONFLICT,
                "EMPLOYEE_DUPLICATE_NAME",
                "Ya existe un funcionario con el nombre: " + name
        );
    }

    public static EmployeeException duplicateIdentification(String identification) {
        return new EmployeeException(
                HttpStatus.CONFLICT,
                "EMPLOYEE_DUPLICATE_IDENTIFICATION",
                "Ya existe un funcionario con la identificación: " + identification
        );
    }

    public static EmployeeException inUse(String name) {
        return new EmployeeException(
                HttpStatus.BAD_REQUEST,
                "EMPLOYEE_IN_USE",
                "No se puede eliminar el funcionario '" + name + "' porque tiene activos asociados"
        );
    }
}
