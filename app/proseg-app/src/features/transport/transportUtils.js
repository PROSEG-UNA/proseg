export const DRIVER_STATUS_OPTIONS = [
    { value: 'ACTIVE', label: 'Activo' },
    { value: 'INACTIVE', label: 'Inactivo' },
];

export const DRIVER_UNIT_OPTIONS = [
    { value: 'GENERAL_SERVICES', label: 'Servicios Generales' },
    { value: 'OTHER_UNIT', label: 'Otra Unidad' },
];

export const ALL_FILTER_VALUE = 'ALL';

export function matchesDriverFilters(driver, unit, status) {
    return (unit === ALL_FILTER_VALUE || driver.organizationalUnit === unit)
        && (status === ALL_FILTER_VALUE || driver.status === status);
}

export const VEHICLE_TYPE_OPTIONS = [
    { value: 'MICROBUS', label: 'Microbús' },
    { value: 'SEDAN', label: 'Sedán' },
    { value: 'ALL_TERRAIN', label: 'Todo Terreno' },
    { value: 'SPECIAL', label: 'Especial' },
    { value: 'PICK_UP', label: 'Pick-Up' },
    { value: 'STATION_WAGON', label: 'Station Wagon' },
];

export const ULLOA_SHIFT_VALUES = ['SHORT_MORNING', 'MORNING', 'AFTERNOON'];

export const ORDINARY_SCHEDULE_OPTIONS = [
    { value: '07:30-16:30', label: '07:30 a 16:30', start: '07:30', end: '16:30' },
    { value: '12:00-20:00', label: '12:00 a 20:00', start: '12:00', end: '20:00' },
];
export const DEFAULT_ORDINARY_START = ORDINARY_SCHEDULE_OPTIONS[0].start;
export const DEFAULT_ORDINARY_END = ORDINARY_SCHEDULE_OPTIONS[0].end;
const AFTERNOON_SCHEDULE_FIRST_MINUTE = 10 * 60;

const HOURS_FORMATTER = new Intl.NumberFormat('es-CR', { maximumFractionDigits: 1 });
const PERCENT_FORMATTER = new Intl.NumberFormat('es-CR', { style: 'percent', minimumFractionDigits: 1, maximumFractionDigits: 1 });

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

export function formatHours(value) {
    return HOURS_FORMATTER.format(value ?? 0);
}

export function formatHourOfDay(value) {
    if (value === null || value === undefined) return '—';
    let wholeHour = Math.trunc(value);
    let minute = Math.round((value - wholeHour) * 60);
    if (minute === 60) {
        wholeHour += 1;
        minute = 0;
    }
    wholeHour = ((wholeHour % 24) + 24) % 24;
    return `${String(wholeHour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

export function formatPercent(ratio) {
    if (ratio === null || ratio === undefined) return '—';
    return PERCENT_FORMATTER.format(ratio);
}

export function driverStatusLabel(value) {
    return findStatusLabel(value, DRIVER_STATUS_OPTIONS);
}

export function driverUnitLabel(value) {
    return findStatusLabel(value, DRIVER_UNIT_OPTIONS);
}

export function vehicleTypeLabel(value) {
    return findStatusLabel(value, VEHICLE_TYPE_OPTIONS);
}

export function toTimeInput(value, fallback = '') {
    return value ? String(value).slice(0, 5) : fallback;
}

export function timeToMinutes(value) {
    const [hours, minutes] = String(value ?? '').split(':').map(Number);
    if (Number.isNaN(hours) || Number.isNaN(minutes)) return null;
    return hours * 60 + minutes;
}

export function ordinaryScheduleValueOf(start) {
    const startMinutes = timeToMinutes(start);
    const afternoon = startMinutes !== null && startMinutes >= AFTERNOON_SCHEDULE_FIRST_MINUTE;
    return ORDINARY_SCHEDULE_OPTIONS[afternoon ? 1 : 0].value;
}

export function getStatusChipColor(value) {
    switch (value) {
        case 'ACTIVE':
            return 'success';
        case 'INACTIVE':
            return 'error';
        default:
            return 'default';
    }
}

export function downloadBlob(blob, fileName) {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(objectUrl);
}
