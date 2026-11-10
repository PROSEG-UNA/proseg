const DEDICATED_WARNING_CODES = new Set(['PRIORITY_ONE_UNATTENDED', 'PLATE_REPEATED_SAME_DAY']);

function groupCount(issues) {
    const ids = new Set();
    let fallback = 0;
    issues.forEach((issue) => {
        const tours = issue?.tours ?? [];
        if (tours.length) tours.forEach((tour) => ids.add(tour.id));
        else fallback += issue?.details?.length ?? 0;
    });
    return ids.size + fallback;
}

export function finalCommissionGroups(check) {
    const errors = check?.errors ?? [];
    const warnings = check?.warnings ?? [];
    const blockers = check?.blockers ?? [];
    const incomplete = check?.rejections
        ? [check.rejections]
        : blockers.filter((issue) => issue.code === 'CLOSED_WITH_INCOMPLETE_TOURS');
    return [
        {
            key: 'oversized',
            title: 'Giras que necesitan más de un viaje',
            issues: errors.filter((issue) => issue.code === 'OVERSIZED'),
        },
        {
            key: 'priority',
            title: 'Giras de prioridad 1 con menos de 5 pasajeros sin atender',
            issues: warnings.filter((issue) => issue.code === 'PRIORITY_ONE_UNATTENDED'),
        },
        {
            key: 'incomplete',
            title: 'Giras sin atender',
            issues: incomplete,
        },
        {
            key: 'plate',
            title: 'Placa repetida el mismo día',
            issues: warnings.filter((issue) => issue.code === 'PLATE_REPEATED_SAME_DAY'),
        },
        {
            key: 'rules',
            title: 'Reglas que se están rompiendo',
            issues: [
                ...blockers.filter((issue) => issue.code !== 'CLOSED_WITH_INCOMPLETE_TOURS'),
                ...errors.filter((issue) => issue.code !== 'OVERSIZED'),
                ...warnings.filter((issue) => !DEDICATED_WARNING_CODES.has(issue.code)),
            ],
        },
    ].map((group) => ({ ...group, count: groupCount(group.issues) }))
        .filter((group) => group.issues.length > 0);
}

export function hasFinalCommissionFindings(check) {
    return finalCommissionGroups(check).length > 0;
}
