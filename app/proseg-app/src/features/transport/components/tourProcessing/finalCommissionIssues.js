export const FINAL_COMMISSION_SECTIONS = [
    { key: 'errors', heading: 'Errores en el borrador', color: 'error', acknowledgeLabel: 'Entiendo este error y quiero descargar de todos modos' },
    { key: 'warnings', heading: 'Reglas que se están rompiendo', color: 'warning', acknowledgeLabel: 'Confirmo que se está rompiendo esta regla' },
];

export const REJECTIONS_KEY = 'rejections';

export function finalCommissionIssueKeys(check) {
    const sectionKeys = FINAL_COMMISSION_SECTIONS.flatMap((section) => (check?.[section.key] ?? []).map((issue) => `${section.key}:${issue.code}`));
    return check?.rejections ? [...sectionKeys, REJECTIONS_KEY] : sectionKeys;
}

export function hasFinalCommissionFindings(check) {
    return Boolean(check?.blockers?.length) || finalCommissionIssueKeys(check).length > 0;
}
