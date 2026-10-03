export const FORM_TYPES = {
    OVERTIME_REPORT: 'OVERTIME_REPORT',
    ABSENCE_REPORT: 'ABSENCE_REPORT',
    LATE_ARRIVAL_REPORT: 'LATE_ARRIVAL_REPORT',
    EQUIPMENT_DELIVERY: 'EQUIPMENT_DELIVERY',
    POST_ANOMALIES: 'POST_ANOMALIES',
    SUPERVISOR_REPORT: 'SUPERVISOR_REPORT',
    SHIFT_CHANGE: 'SHIFT_CHANGE',
    VACATION_PERMIT_REQUEST: 'VACATION_PERMIT_REQUEST',
    SERVICE_ROSTER: 'SERVICE_ROSTER',
    ACCESS_CONTROL: 'ACCESS_CONTROL',
    DAILY_ROUNDS_CONTROL: 'DAILY_ROUNDS_CONTROL',
    LOGBOOK: 'LOGBOOK',
};

export const formRegistry = [
    { code: FORM_TYPES.OVERTIME_REPORT, name: 'Reporte de Horas Extras' },
    { code: FORM_TYPES.ABSENCE_REPORT, name: 'Reporte por Ausencia' },
    { code: FORM_TYPES.LATE_ARRIVAL_REPORT, name: 'Reporte por Llegada Tardía' },
    { code: FORM_TYPES.EQUIPMENT_DELIVERY, name: 'Entrega de Equipo' },
    { code: FORM_TYPES.POST_ANOMALIES, name: 'Anomalías en los Puestos' },
    { code: FORM_TYPES.SUPERVISOR_REPORT, name: 'Reporte del/de la Supervisor/a' },
    { code: FORM_TYPES.SHIFT_CHANGE, name: 'Cambio de Jornada' },
    { code: FORM_TYPES.VACATION_PERMIT_REQUEST, name: 'Solicitud de Vacaciones y Otros Permisos' },
    { code: FORM_TYPES.SERVICE_ROSTER, name: 'Rol de Servicio' },
    { code: FORM_TYPES.ACCESS_CONTROL, name: 'Control de Acceso e Ingreso' },
    { code: FORM_TYPES.DAILY_ROUNDS_CONTROL, name: 'Control de Rondas Diarias' },
    { code: FORM_TYPES.LOGBOOK, name: 'Bitácora' },
];
