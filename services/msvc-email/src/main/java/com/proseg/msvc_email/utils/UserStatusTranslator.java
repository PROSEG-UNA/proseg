package com.proseg.msvc_email.utils;

public class UserStatusTranslator {
    
    public static String translateStatus(String status) {
        if (status == null) {
            return status;
        }
        
        return switch (status.toUpperCase()) {
            case "INVITED" -> "Invitados";
            case "APPROVED" -> "Activos";
            case "REJECTED" -> "Inactivos";
            case "PENDING" -> "Pendientes";
            default -> status;
        };
    }
}
