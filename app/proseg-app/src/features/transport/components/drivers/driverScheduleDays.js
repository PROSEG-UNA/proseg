import { ORDINARY_SCHEDULE_OPTIONS, ordinaryScheduleValueOf } from '../../transportUtils';

export function dayOverridesFromScheduleDays(scheduleDays) {
    return (scheduleDays ?? []).reduce((overrides, scheduleDay) => ({
        ...overrides,
        [Number(String(scheduleDay.date).slice(8, 10))]: ordinaryScheduleValueOf(scheduleDay.ordinaryStart),
    }), {});
}

export function sameDayOverrides(left, right) {
    const leftKeys = Object.keys(left);
    return leftKeys.length === Object.keys(right).length && leftKeys.every((key) => left[key] === right[key]);
}

export function scheduleDaysPayload(dayOverrides, baseValue, period) {
    const monthPrefix = `${period.year}-${String(period.month).padStart(2, '0')}`;
    return Object.entries(dayOverrides)
        .filter(([, value]) => value !== baseValue)
        .sort(([leftDay], [rightDay]) => Number(leftDay) - Number(rightDay))
        .map(([day, value]) => {
            const option = ORDINARY_SCHEDULE_OPTIONS.find((candidate) => candidate.value === value);
            return {
                date: `${monthPrefix}-${String(day).padStart(2, '0')}`,
                ordinaryStart: option.start,
                ordinaryEnd: option.end,
            };
        });
}
