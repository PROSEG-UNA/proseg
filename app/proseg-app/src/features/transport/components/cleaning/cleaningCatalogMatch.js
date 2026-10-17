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
