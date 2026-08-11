import { formatDate, formatDateTime } from '../../common/utils/formatters.js';

export const MAINTENANCE_STATUS_OPTIONS = [
    { value: 'PENDING', label: 'Pendiente' },
    { value: 'COMPLETED', label: 'Completada' },
    { value: 'CANCELLED', label: 'Cancelada' },
];

export const MAINTENANCE_STATUS_TRANSITIONS = {
    PENDING: ['COMPLETED', 'CANCELLED'],
    COMPLETED: [],
    CANCELLED: [],
};

export function canTransitionMaintenanceStatus(from, to) {
    if (!from || !to) return false;
    if (from === to) return true;
    return (MAINTENANCE_STATUS_TRANSITIONS[from] ?? []).includes(to);
}

export function maintenanceStatusOptionsFor(currentStatus) {
    return MAINTENANCE_STATUS_OPTIONS.filter(
        (option) => canTransitionMaintenanceStatus(currentStatus, option.value)
    );
}

export const MAINTENANCE_TICKET_STATUS_OPTIONS = [
    { value: 'OPEN', label: 'Abierto' },
    { value: 'IN_PROGRESS', label: 'En Progreso' },
    { value: 'RESOLVED', label: 'Resuelto' },
    { value: 'REOPENED', label: 'Reabierto' },
    { value: 'CANCELLED', label: 'Cancelado' },
];

export const MAINTENANCE_PRIORITY_OPTIONS = [
    { value: 'LOW', label: 'Baja' },
    { value: 'MEDIUM', label: 'Media' },
    { value: 'HIGH', label: 'Alta' }
];

export { formatDate, formatDateTime };

export function normalizeTextList(input) {
    return Array.from(
        new Set(
            String(input ?? '')
                .split(/[\n,;]+/)
                .map((item) => item.trim())
                .filter(Boolean)
        )
    );
}

export function joinTextList(values) {
    return Array.isArray(values) ? values.filter(Boolean).join('\n') : '';
}

export function checkScheduleConsistency({ startDate, endDate, startTime, endTime }) {
    let endDateInvalid = false;
    let endTimeInvalid = false;
    if (startDate && endDate) {
        if (endDate.isBefore(startDate, 'day')) {
            endDateInvalid = true;
        } else if (endDate.isSame(startDate, 'day') && startTime && endTime && !endTime.isAfter(startTime)) {
            endTimeInvalid = true;
        }
    }
    return { endDateInvalid, endTimeInvalid };
}

export function statusLabel(value) {
    return MAINTENANCE_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value ?? '—';
}

export function priorityLabel(value) {
    return MAINTENANCE_PRIORITY_OPTIONS.find((option) => option.value === value)?.label ?? value ?? '—';
}

export const formatDateHourMinute = (value) => {
    if (!value) return '';

    return new Date(value).toLocaleString('es-CR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
};