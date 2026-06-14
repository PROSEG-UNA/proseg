package com.sssi.msvc_maintenance.security;

public final class Privileges {

    private Privileges() {}

    public static final class Empresas {
        public static final String LEER       = "LEER_EMPRESAS";
        public static final String GESTIONAR  = "GESTIONAR_EMPRESAS";
        public static final String ELIMINAR   = "ELIMINAR_EMPRESAS";
    }

    public static final class SolicitudesMantenimiento {
        public static final String LEER                = "LEER_SOLICITUDES_MANTENIMIENTO";
        public static final String SOLICITAR           = "SOLICITAR_MANTENIMIENTO";
        public static final String EDITAR              = "EDITAR_SOLICITUDES_MANTENIMIENTO";
        public static final String SELECCIONAR_EMPRESA = "SELECCIONAR_EMPRESA_EN_SOLICITUD_MANTENIMIENTO";
        public static final String ELIMINAR            = "ELIMINAR_SOLICITUDES_MANTENIMIENTO";
    }

    public static final class TecnicosMantenimiento {
        public static final String LEER       = "LEER_TECNICOS_MANTENIMIENTO";
        public static final String GESTIONAR  = "GESTIONAR_TECNICOS_MANTENIMIENTO";
        public static final String ELIMINAR   = "ELIMINAR_TECNICOS_MANTENIMIENTO";
    }

    public static final class UsuariosEmpresas {
        public static final String LEER       = "LEER_USUARIOS_EMPRESAS";
        public static final String GESTIONAR  = "GESTIONAR_USUARIOS_EMPRESAS";
        public static final String ELIMINAR   = "ELIMINAR_USUARIOS_EMPRESAS";
    }

    public static final class RegistrosMantenimiento {
        public static final String LEER       = "LEER_REGISTROS_MANTENIMIENTO";
        public static final String GESTIONAR  = "GESTIONAR_REGISTROS_MANTENIMIENTO";
        public static final String HISTORIAL  = "LEER_HISTORIAL_REGISTROS_MANTENIMIENTO";
    }

    public static final class Tickets {
        public static final String LEER       = "LEER_TICKET_MANTENIMIENTO";
        public static final String CREAR      = "CREAR_TICKETS_MANTENIMIENTO";
        public static final String EDITAR     = "EDITAR_TICKETS_MANTENIMIENTO";
        public static final String COMENTAR   = "COMENTAR_TICKETS_MANTENIMIENTO";
        public static final String VER_TODOS  = "LEER_TODOS_TICKETS_MANTENIMIENTO";
        public static final String ASIGNAR_PRIORIDAD = "ASIGNAR_PRIORIDAD_TICKETS_MANTENIMIENTO";
        public static final String ELIMINAR   = "ELIMINAR_TICKETS_MANTENIMIENTO";
    }
}