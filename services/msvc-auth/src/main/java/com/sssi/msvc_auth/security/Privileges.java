package com.sssi.msvc_auth.security;

public final class Privileges {

    private Privileges() {}

    public static final class Role {
        public static final String READ_BASE = "LEER_ROLES_BASE";
        public static final String READ_COMPOSITE = "LEER_ROLES_COMPUESTOS";
        public static final String READ_ROLE_COMPOSITES = "LEER_COMPOSITES_ROL";
        public static final String CREATE = "CREAR_ROL";
        public static final String UPDATE = "EDITAR_ROL";
        public static final String DELETE = "ELIMINAR_ROL";
        public static final String READ_USERS_BY_ROLE = "LEER_USUARIOS_POR_ROL";
    }

    public static final class User {
        public static final String CREATE = "CREAR_USUARIO";
        public static final String READ = "LEER_USUARIO";
        public static final String READ_ALL = "LEER_USUARIOS";
        public static final String READ_ROLES = "LEER_ROLES_USUARIO";
        public static final String APPROVE = "APROBAR_USUARIO";
        public static final String ASSIGN_ROLE = "ASIGNAR_ROL_USUARIO";
        public static final String REMOVE_ROLE = "ELIMINAR_ROL_USUARIO";
    }

    public static final class Invitation {
        public static final String READ_PENDING = "LEER_INVITACIONES_PENDIENTES";
    }
}