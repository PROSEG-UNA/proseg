export function draftStatusLabel(value) {
    if (value === 'DRAFT') return 'Borrador';
    if (value === 'REGISTERED') return 'Registrado';
    return value || '—';
}

const MONTH_NAMES = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Setiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

export function draftPeriodLabel(draft) {
    if (!draft?.year || !draft?.month) return '—';
    return `${MONTH_NAMES[draft.month - 1] ?? draft.month} ${draft.year}`;
}

export function shortPeriodLabel(year, month) {
    return `${MONTH_NAMES[month - 1] ?? month} ${String(year).slice(-2)}`;
}

export function draftRowDuplicateLabel(row) {
    if (row.conflicting) return 'Conflicto';
    if (row.duplicateGroupSize > 1) return `Repetida ${row.duplicateRank}/${row.duplicateGroupSize}`;
    return null;
}

export function draftStatusColor(value) {
    if (value === 'DRAFT') return 'warning';
    if (value === 'REGISTERED') return 'success';
    return 'default';
}
