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

    public static final class VehicleMaintenance {
        public static final String LEER = "LEER_MANTENIMIENTO_VEHICULOS";
        public static final String GESTIONAR = "GESTIONAR_MANTENIMIENTO_VEHICULOS";
        public static final String ELIMINAR = "ELIMINAR_MANTENIMIENTO_VEHICULOS";
    }

    public static final class Tours {
        public static final String LEER = "LEER_GIRAS";
        public static final String GESTIONAR = "GESTIONAR_GIRAS";
        public static final String ELIMINAR = "ELIMINAR_GIRAS";
    }

    public static final class Assignments {
        public static final String GENERAR = "GENERAR_ASIGNACIONES";
        public static final String ACTUALIZAR = "ACTUALIZAR_ASIGNACIONES";
    }
}
