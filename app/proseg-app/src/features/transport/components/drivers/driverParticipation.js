export const PARTICIPATION_FILTER_OPTIONS = [
    { value: 'participated', label: 'Choferes que participaron en el mes' },
    { value: 'all', label: 'Todos' },
];

export const PARTICIPATION_FILTER_VALUES = PARTICIPATION_FILTER_OPTIONS.map((option) => option.value);

export function hasMonthParticipation(driverMonth) {
    if (!driverMonth) return false;
    const accruedHours = (driverMonth.ordinaryHoursAccrued ?? 0)
        + (driverMonth.surplusHoursAccrued ?? 0)
        + (driverMonth.overtimeHoursAccrued ?? 0);
    return accruedHours > 0 || (driverMonth.weekendsWorked ?? 0) > 0;
}

export function participatingDriverIds(byDriverId) {
    if (!byDriverId) return [];
    return [...byDriverId.values()]
        .filter(hasMonthParticipation)
        .map((driverMonth) => driverMonth.driverId)
        .sort();
}
