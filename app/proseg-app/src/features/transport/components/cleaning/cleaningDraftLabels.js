export function draftStatusLabel(value) {
    if (value === 'DRAFT') return 'Borrador';
    if (value === 'REGISTERED') return 'Registrado';
    return value || '—';
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
