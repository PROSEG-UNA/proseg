package com.sssi.msvc_maintenance.security;

public final class Privileges {

    private Privileges() {}

    public static final class Empresas {
        public static final String LEER       = "LEER_EMPRESAS";
        public static final String GESTIONAR  = "GESTIONAR_EMPRESAS";
        public static final String ELIMINAR   = "ELIMINAR_EMPRESAS";
    }

    public static final class SolicitudesMantenimiento {
        public static final String LEER       = "LEER_SOLICITUDES_MANTENIMIENTO";
        public static final String GESTIONAR  = "GESTIONAR_SOLICITUDES_MANTENIMIENTO";
        public static final String ELIMINAR   = "ELIMINAR_SOLICITUDES_MANTENIMIENTO";
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
}