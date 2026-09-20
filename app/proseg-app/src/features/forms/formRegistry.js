export const FORM_TYPES = {
    OVERTIME_REPORT: 'OVERTIME_REPORT',
    ABSENCE_REPORT: 'ABSENCE_REPORT',
    LATE_ARRIVAL_REPORT: 'LATE_ARRIVAL_REPORT',
};

export const formRegistry = [
    { code: FORM_TYPES.OVERTIME_REPORT, name: 'Reporte de Horas Extras' },
    { code: FORM_TYPES.ABSENCE_REPORT, name: 'Reporte por Ausencia' },
    { code: FORM_TYPES.LATE_ARRIVAL_REPORT, name: 'Reporte por Llegada Tardía' },
];
