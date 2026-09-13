export function formatDate(value) {
    if (!value) return '—';
    const [year, month, day] = String(value).split('T')[0].split('-');
    if (!year || !month || !day) return value;
    return `${day}/${month}/${year}`;
}

export function formatDateTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat('es-CR', {
        dateStyle: 'short',
        timeStyle: 'short',
    }).format(date);
}

export function formTypeLabel(code) {
    switch (code) {
        case 'OVERTIME_REPORT':
            return 'Reporte de Horas Extras';
        case 'ABSENCE_REPORT':
            return 'Reporte por Ausencia';
        case 'LATE_ARRIVAL_REPORT':
            return 'Reporte por Llegada Tardía';
        default:
            return code ?? '—';
    }
}
