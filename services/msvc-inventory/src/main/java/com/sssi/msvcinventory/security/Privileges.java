package com.sssi.msvcinventory.security;

public final class Privileges {

    private Privileges() {}

    public static final class Activos {
        public static final String LEER     = "LEER_ACTIVOS";
        public static final String GESTIONAR = "GESTIONAR_ACTIVOS";
        public static final String ELIMINAR  = "ELIMINAR_ACTIVOS";
    }

    public static final class Ubicaciones {
        public static final String LEER     = "LEER_UBICACIONES";
        public static final String GESTIONAR = "GESTIONAR_UBICACIONES";
        public static final String ELIMINAR  = "ELIMINAR_UBICACIONES";
    }
}
