package com.proseg.msvc_transport.security;

public final class Privileges {

    private Privileges() {}

    public static final class Drivers {
        public static final String LEER = "LEER_CHOFERES";
        public static final String GESTIONAR = "GESTIONAR_CHOFERES";
        public static final String ELIMINAR = "ELIMINAR_CHOFERES";
    }

    public static final class Vehicles {
        public static final String LEER = "LEER_VEHICULOS";
        public static final String GESTIONAR = "GESTIONAR_VEHICULOS";
        public static final String ELIMINAR = "ELIMINAR_VEHICULOS";
    }

    public static final class Tours {
        public static final String LEER = "LEER_GIRAS";
        public static final String IMPORTAR = "IMPORTAR_GIRAS";
        public static final String CREAR = "CREAR_GIRAS";
        public static final String EDITAR = "EDITAR_GIRAS";
        public static final String ELIMINAR = "ELIMINAR_GIRAS";
        public static final String EXPORTAR = "EXPORTAR_GIRAS";
    }
}
