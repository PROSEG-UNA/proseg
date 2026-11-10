import {
    DEFAULT_ORDINARY_END,
    DEFAULT_ORDINARY_START,
    driverStatusLabel,
    driverUnitLabel,
    toTimeInput,
    vehicleTypeLabel,
} from '../../transportUtils';

export const GENERAL_SERVICES_UNIT = 'GENERAL_SERVICES';
export const OTHER_UNIT = 'OTHER_UNIT';

export function checksDriverCatalog(resourceType) {
    return resourceType !== 'EXTERNAL';
}

const WHITESPACE = /\s+/g;

export const CATALOG_MISSING_MESSAGES = {
    driver: 'Este chofer no se encuentra en el sistema, agrégalo antes de continuar',
    vehicle: 'Este vehículo no se encuentra en el sistema, agrégalo antes de continuar',
    vehicleNotPlate: 'Este valor no es una placa, selecciona un vehículo del sistema o deja el campo vacío',
};

export function driverMatchKey(value) {
    return String(value ?? '').trim().replace(WHITESPACE, ' ').toLowerCase();
}

export function plateMatchKey(value) {
    return String(value ?? '').replace(WHITESPACE, '').toUpperCase();
}

export function looksLikePlate(value) {
    return /\d/.test(String(value ?? ''));
}

export function canAddToCatalog(kind, value) {
    return kind !== 'vehicle' || looksLikePlate(value);
}

export function missingCatalogMessage(kind, value) {
    if (!canAddToCatalog(kind, value)) return CATALOG_MISSING_MESSAGES.vehicleNotPlate;
    return CATALOG_MISSING_MESSAGES[kind];
}

function joinParts(parts) {
    return parts.filter(Boolean).join(' · ');
}

export function buildDriverCatalogEntry(driver) {
    const schedule = `${toTimeInput(driver.ordinaryStart, DEFAULT_ORDINARY_START)} a ${toTimeInput(driver.ordinaryEnd, DEFAULT_ORDINARY_END)}`;
    const unit = driverUnitLabel(driver.unit ?? GENERAL_SERVICES_UNIT);
    const allowedTypes = (driver.allowedVehicleTypes ?? []).map(vehicleTypeLabel).join(', ');
    const restrictions = driver.restrictions?.trim() ?? '';
    const inactive = driver.status === 'INACTIVE';
    return {
        label: driver.label,
        unit: driver.unit ?? GENERAL_SERVICES_UNIT,
        secondary: joinParts([unit, schedule, allowedTypes && `Maneja: ${allowedTypes}`, restrictions && `Restricción: ${restrictions}`]),
        badge: inactive ? { label: driverStatusLabel(driver.status), color: 'error' } : null,
        searchText: joinParts([driver.label, unit, schedule, allowedTypes, restrictions, inactive && driverStatusLabel(driver.status)]),
    };
}

export function buildVehicleCatalogEntry(vehicle) {
    const type = vehicle.type ? vehicleTypeLabel(vehicle.type) : '';
    const capacity = vehicle.capacity != null ? `${vehicle.capacity} pasajeros` : '';
    const unavailable = vehicle.available === false;
    return {
        label: vehicle.label,
        secondary: joinParts([type, capacity]),
        badge: unavailable ? { label: 'No disponible', color: 'error' } : null,
        searchText: joinParts([vehicle.label, type, capacity, unavailable && 'No disponible']),
    };
}

export function catalogEntryLabel(option) {
    return typeof option === 'string' ? option : option?.label ?? '';
}

export function catalogEntryText(option) {
    return typeof option === 'string' ? option : option?.searchText ?? option?.label ?? '';
}
