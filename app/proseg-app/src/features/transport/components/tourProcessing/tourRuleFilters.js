export const TOUR_RULE_FILTERS = [
    { code: 'NEEDS_MULTIPLE_TRIPS', label: 'Necesitan más de un viaje' },
    { code: 'PRIORITY_ONE_UNATTENDED', label: 'Prioridad 1 con menos de 5 pasajeros sin atender' },
    { code: 'INCOMPLETE', label: 'Sin chofer o sin vehículo' },
    { code: 'PLATE_REPEATED_SAME_DAY', label: 'Placa repetida el mismo día' },
    { code: 'PARTIAL_DISTRIBUTION', label: 'Divididas con viajes a medias' },
    { code: 'DRIVER_OVERLAP', label: 'Choferes con giras que se cruzan' },
    { code: 'DAY_HOURS_EXCEEDED', label: 'Choferes con más de 12 horas en un día' },
    { code: 'SURPLUS_CAP_EXCEEDED', label: 'Choferes que superan el tope de sobresueldo' },
    { code: 'OVERTIME_CAP_EXCEEDED', label: 'Choferes que superan las 60 horas extra' },
    { code: 'OVERTIME_WITHOUT_SURPLUS', label: 'Horas extra sin llegar al 95% de sobresueldo' },
    { code: 'WEEKEND_CAP_EXCEEDED', label: 'Choferes que superan el tope de fines de semana' },
    { code: 'INACTIVE_DRIVER', label: 'Choferes inactivos con giras asignadas' },
    { code: 'VEHICLE_OVERLAP', label: 'Vehículos asignados a giras que se cruzan' },
    { code: 'VEHICLE_OVER_CAPACITY', label: 'Viajes con más pasajeros que la capacidad' },
    { code: 'INCOMPATIBLE_VEHICLE', label: 'Chofer en un vehículo que no puede manejar' },
    { code: 'VEHICLE_UNAVAILABLE', label: 'Vehículos no disponibles' },
    { code: 'EXTERNAL_UNDER_MINIMUM', label: 'Contratación externa con menos de 5 pasajeros' },
    { code: 'ANY', label: 'Cualquier regla' },
];

export const NEEDS_MULTIPLE_TRIPS = 'NEEDS_MULTIPLE_TRIPS';
