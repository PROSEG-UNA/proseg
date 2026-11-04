export function draftStatusLabel(value) {
    if (value === 'DRAFT') return 'Borrador';
    if (value === 'CLOSED') return 'Terminada';
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
    if (value === 'CLOSED') return 'success';
    if (value === 'REGISTERED') return 'success';
    return 'default';
}

export const APPROVAL_STATUS = { APPROVED: 'APPROVED', NOT_APPROVED: 'NOT_APPROVED' };

export const APPROVAL_STATUS_FILTER_OPTIONS = [
    { label: 'Aprobada', value: APPROVAL_STATUS.APPROVED },
    { label: 'No aprobada', value: APPROVAL_STATUS.NOT_APPROVED },
];

export function approvalStatusLabel(value) {
    return value === APPROVAL_STATUS.NOT_APPROVED ? 'No aprobada' : 'Aprobada';
}

export function approvalStatusColor(value) {
    return value === APPROVAL_STATUS.NOT_APPROVED ? 'error' : 'success';
}

export function toggledApprovalStatus(value) {
    return value === APPROVAL_STATUS.NOT_APPROVED ? APPROVAL_STATUS.APPROVED : APPROVAL_STATUS.NOT_APPROVED;
}
