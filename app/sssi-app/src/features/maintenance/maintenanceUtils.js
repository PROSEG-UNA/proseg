export const MAINTENANCE_STATUS_OPTIONS = [
    { value: 'PENDING', label: 'Pendiente' },
    { value: 'IN_PROGRESS', label: 'En progreso' },
    { value: 'COMPLETED', label: 'Completada' },
    { value: 'CANCELLED', label: 'Cancelada' },
];

export const MAINTENANCE_PRIORITY_OPTIONS = [
    { value: 'LOW', label: 'Baja' },
    { value: 'MEDIUM', label: 'Media' },
    { value: 'HIGH', label: 'Alta' },
    { value: 'CRITICAL', label: 'Crítica' },
];

export function formatDate(value) {
    if (!value) return '—';
    const [year, month, day] = String(value).split('-');
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

export function statusLabel(value) {
    return MAINTENANCE_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value ?? '—';
}

export function priorityLabel(value) {
    return MAINTENANCE_PRIORITY_OPTIONS.find((option) => option.value === value)?.label ?? value ?? '—';
}

