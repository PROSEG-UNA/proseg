export const formatRoleName = (name) => {
    if (!name) return name;
    return name
        .split('_')
        .map((word, i) => i === 0
            ? word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
            : word.toLowerCase()
        )
        .join(' ');
};

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

const EMPTY_VALUE_PLACEHOLDER = '—';

export function formatAssetIdentity(asset) {
    if (!asset) return '';
    const serialNumber = asset.serialNumber;
    const hasSerialNumber = !!serialNumber && serialNumber !== EMPTY_VALUE_PLACEHOLDER;
    return hasSerialNumber
        ? `Activo ${asset.assetNumber} · Serie ${serialNumber}`
        : `Activo ${asset.assetNumber}`;
}
