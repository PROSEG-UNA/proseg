import { fieldChangeText } from '../../fieldChangeText.js';

function textOf(value) {
    return value === null || value === undefined ? '' : String(value).trim();
}

function availabilityLabel(value) {
    return value === 'true' ? 'Disponible' : 'No disponible';
}

export function describeExternalVehicleChanges(before, after) {
    return [
        fieldChangeText('Proveedor', textOf(before.provider), textOf(after.provider)),
        fieldChangeText('Placa', textOf(before.plate), textOf(after.plate)),
        fieldChangeText('Capacidad', textOf(before.capacity), textOf(after.capacity)),
        fieldChangeText('Estado', String(Boolean(before.available)), String(Boolean(after.available)), availabilityLabel),
    ].filter(Boolean);
}
