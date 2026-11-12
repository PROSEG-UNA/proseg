import { fieldChangeText } from '../../fieldChangeText.js';
import { vehicleTypeLabel } from '../../transportUtils.js';

function textOf(value) {
    return value === null || value === undefined ? '' : String(value).trim();
}

function availabilityLabel(value) {
    return value === 'true' ? 'Disponible' : 'No disponible';
}

export function describeVehicleChanges(before, after) {
    const changes = [
        fieldChangeText('Placa', textOf(before.plate), textOf(after.plate)),
        fieldChangeText('Tipo', textOf(before.type), textOf(after.type), vehicleTypeLabel),
        fieldChangeText('Capacidad', textOf(before.capacity), textOf(after.capacity)),
    ];
    if ('available' in after) {
        changes.push(fieldChangeText('Estado', String(Boolean(before.available)), String(Boolean(after.available)), availabilityLabel));
    }
    return changes.filter(Boolean);
}

export function vehicleSavedMessage({ kind = 'Vehículo', plate, created, changes }) {
    if (created) return `${kind} ${plate} creado correctamente.`;
    if (!changes.length) return `${kind} ${plate} guardado. No hubo cambios en sus datos.`;
    return `${kind} ${plate} actualizado correctamente.\n\n${changes.join('\n')}`;
}
