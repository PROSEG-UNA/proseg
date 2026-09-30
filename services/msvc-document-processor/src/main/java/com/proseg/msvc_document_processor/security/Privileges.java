package com.proseg.msvc_document_processor.security;

public final class Privileges {

    private Privileges() {}

    public static final class Activos {
        public static final String IMPORTAR = "IMPORTAR_ACTIVOS";
        public static final String EXPORTAR = "EXPORTAR_ACTIVOS";
    }

    public static final class Asignaciones {
        public static final String GENERAR = "GENERAR_ASIGNACIONES";
        public static final String ACTUALIZAR = "ACTUALIZAR_ASIGNACIONES";
    }

    public static final class SolicitudesMantenimiento {
        public static final String LEER = "LEER_SOLICITUDES_MANTENIMIENTO";
    }

    public static final class TicketsMantenimiento {
        public static final String LEER = "LEER_TICKET_MANTENIMIENTO";
    }
}
