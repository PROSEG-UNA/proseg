export const DRIVER_STATUS_OPTIONS = [
    { value: 'ACTIVE', label: 'Activo' },
    { value: 'INACTIVE', label: 'Inactivo' },
];

export const VEHICLE_STATUS_OPTIONS = [
    { value: 'AVAILABLE', label: 'Disponible' },
    { value: 'IN_USE', label: 'En uso' },
    { value: 'MAINTENANCE', label: 'En mantenimiento' },
    { value: 'OUT_OF_SERVICE', label: 'Fuera de servicio' },
];

export const VEHICLE_MAINTENANCE_STATUS_OPTIONS = [
    { value: 'SCHEDULED', label: 'Programado' },
    { value: 'IN_PROGRESS', label: 'En progreso' },
    { value: 'COMPLETED', label: 'Completado' },
    { value: 'CANCELLED', label: 'Cancelado' },
];

export const TOUR_STATUS_OPTIONS = [
    { value: 'PLANNED', label: 'Planificada' },
    { value: 'IN_PROGRESS', label: 'En progreso' },
    { value: 'COMPLETED', label: 'Completada' },
    { value: 'CANCELLED', label: 'Cancelada' },
];

export const ASSIGNMENT_STATUS_OPTIONS = [
    { value: 'PENDING', label: 'Pendiente' },
    { value: 'ASSIGNED', label: 'Asignada' },
    { value: 'COMPLETED', label: 'Completada' },
    { value: 'CANCELLED', label: 'Cancelada' },
];

function findStatusLabel(value, options) {
    return options.find((option) => option.value === value)?.label ?? value ?? '—';
}

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

export function formatCurrency(value) {
    if (value === null || value === undefined || value === '') return '—';
    const amount = Number(value);
    if (Number.isNaN(amount)) return value;
    return new Intl.NumberFormat('es-CR', {
        style: 'currency',
        currency: 'CRC',
        maximumFractionDigits: 2,
    }).format(amount);
}

export function driverStatusLabel(value) {
    return findStatusLabel(value, DRIVER_STATUS_OPTIONS);
}

export function vehicleStatusLabel(value) {
    return findStatusLabel(value, VEHICLE_STATUS_OPTIONS);
}

export function maintenanceStatusLabel(value) {
    return findStatusLabel(value, VEHICLE_MAINTENANCE_STATUS_OPTIONS);
}

export function tourStatusLabel(value) {
    return findStatusLabel(value, TOUR_STATUS_OPTIONS);
}

export function assignmentStatusLabel(value) {
    return findStatusLabel(value, ASSIGNMENT_STATUS_OPTIONS);
}

export function getStatusChipColor(value) {
    switch (value) {
        case 'ACTIVE':
        case 'AVAILABLE':
        case 'COMPLETED':
        case 'ASSIGNED':
            return 'success';
        case 'IN_PROGRESS':
        case 'IN_USE':
            return 'info';
        case 'SCHEDULED':
        case 'PLANNED':
        case 'PENDING':
            return 'warning';
        case 'CANCELLED':
        case 'INACTIVE':
        case 'OUT_OF_SERVICE':
            return 'error';
        case 'MAINTENANCE':
            return 'secondary';
        default:
            return 'default';
    }
}
