import { formatDate } from '../../transportUtils.js';
import { APPROVAL_STATUS, approvalStatusLabel } from './tourProcessingDraftLabels.js';

const MAX_TEXT_LENGTH = 40;

const RESOURCE_TYPE_LABELS = {
    INTERNAL: 'Interno',
    EXTERNAL: 'Contratación externa',
    BORROWED_VEHICLE: 'Préstamo de vehículo',
};

function textOf(value) {
    return value === null || value === undefined ? '' : String(value).trim();
}

function timeOf(value) {
    return textOf(value).slice(0, 5);
}

function shorten(text) {
    return text.length > MAX_TEXT_LENGTH ? `${text.slice(0, MAX_TEXT_LENGTH)}...` : text;
}

const FIELDS = [
    { key: 'number', label: 'Número', read: textOf },
    { key: 'passengers', label: 'Pasajeros', read: textOf },
    { key: 'executingUnit', label: 'Unidad ejecutora', read: textOf },
    { key: 'responsible', label: 'Responsable', read: textOf },
    { key: 'destination', label: 'Destinos', read: textOf },
    { key: 'durationDays', label: 'Duración (días)', read: textOf },
    { key: 'priority', label: 'Prioridad', read: textOf },
    { key: 'modality', label: 'Modalidad', read: textOf },
    { key: 'departureDate', label: 'Fecha salida', read: textOf, display: formatDate },
    { key: 'departureTime', label: 'Hora salida', read: timeOf },
    { key: 'returnDate', label: 'Fecha regreso', read: textOf, display: formatDate },
    { key: 'returnTime', label: 'Hora regreso', read: timeOf },
    { key: 'observations', label: 'Observaciones', read: textOf },
];

const SINGLE_TRIP_FIELDS = [
    { key: 'resourceType', label: 'Tipo de recurso', read: (value) => textOf(value) || 'INTERNAL', display: (value) => RESOURCE_TYPE_LABELS[value] ?? value },
    { key: 'driver', label: 'Chofer', read: textOf },
    { key: 'vehicle', label: 'Vehículo', read: textOf },
];

function assignmentSignature(assignment) {
    return [
        textOf(assignment.driver),
        textOf(assignment.vehicle),
        textOf(assignment.resourceType) || 'INTERNAL',
        textOf(assignment.borrowedVehicleId),
        textOf(assignment.passengers),
    ].join('|');
}

function describeValue(field, value) {
    return shorten(field.display ? field.display(value) : value);
}

function compareField(field, row, payload) {
    const before = field.read(row[field.key]);
    const after = field.read(payload[field.key]);
    if (before === after) return null;
    if (!before) return `${field.label}: se agregó ${describeValue(field, after)}`;
    if (!after) return `${field.label}: se quitó (antes era ${describeValue(field, before)})`;
    return `${field.label}: de ${describeValue(field, before)} a ${describeValue(field, after)}`;
}

function tripLine(assignment, index) {
    const type = textOf(assignment.resourceType) || 'INTERNAL';
    const driver = textOf(assignment.driver) || 'sin chofer';
    let vehicle = textOf(assignment.vehicle) || 'sin vehículo';
    if (type === 'BORROWED_VEHICLE') vehicle = 'vehículo prestado';
    const passengers = Number(assignment.passengers);
    return `Viaje ${index + 1}: ${driver}, ${vehicle}, ${passengers} ${passengers === 1 ? 'pasajero' : 'pasajeros'}`;
}

function describeTripsChange(rowAssignments, payloadAssignments) {
    const before = rowAssignments?.length ?? 0;
    const after = payloadAssignments?.length ?? 0;
    let header = 'Viajes: se modificaron';
    if (before === 0) header = `Viajes: ahora son ${after} (antes era un solo viaje)`;
    else if (after === 0) header = `Viajes: ahora es un solo viaje (antes eran ${before})`;
    else if (before !== after) header = `Viajes: ahora son ${after} (antes eran ${before})`;
    return [header, ...(payloadAssignments ?? []).map(tripLine)];
}

function sameTrips(rowAssignments, payloadAssignments) {
    const before = (rowAssignments ?? []).map(assignmentSignature);
    const after = (payloadAssignments ?? []).map(assignmentSignature);
    return before.length === after.length && before.every((value, index) => value === after[index]);
}

export function describeDraftRowChanges(row, payload) {
    const changes = [];
    const hasTrips = Boolean(row.assignments?.length) || Boolean(payload.assignments?.length);

    FIELDS.forEach((field) => {
        const change = compareField(field, row, payload);
        if (change) changes.push(change);
    });

    if (hasTrips) {
        if (!sameTrips(row.assignments, payload.assignments)) {
            changes.push(...describeTripsChange(row.assignments, payload.assignments));
        }
    } else {
        SINGLE_TRIP_FIELDS.forEach((field) => {
            const change = compareField(field, row, payload);
            if (change) changes.push(change);
        });
        if (payload.resourceType === 'BORROWED_VEHICLE' && textOf(row.borrowedVehicleId) !== textOf(payload.borrowedVehicleId)) {
            changes.push('Vehículo prestado: modificado');
        }
    }

    const approvalBefore = row.approvalStatus ?? APPROVAL_STATUS.APPROVED;
    if (payload.approvalStatus && payload.approvalStatus !== approvalBefore) {
        changes.push(`Estado: de ${approvalStatusLabel(approvalBefore)} a ${approvalStatusLabel(payload.approvalStatus)}`);
    }

    return changes;
}
