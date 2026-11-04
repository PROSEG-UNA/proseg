import { plateMatchKey } from './tourProcessingCatalogMatch.js';

export function vehicleDistributionWarning(row, vehicles, borrowedVehicles) {
    if (row.assignments?.length > 1 || row.resourceType === 'EXTERNAL') return null;

    const passengers = Number(row.passengers);
    if (!Number.isInteger(passengers) || passengers < 2) return null;

    let capacity;
    if (row.resourceType === 'BORROWED_VEHICLE') {
        capacity = borrowedVehicles.find((vehicle) => vehicle.id === row.borrowedVehicleId)?.capacity;
    } else {
        const selectedVehicle = vehicles.find((vehicle) => plateMatchKey(vehicle.label) === plateMatchKey(row.vehicle));
        const maximumCapacity = Math.max(0, ...vehicles
            .filter((vehicle) => vehicle.available !== false && vehicle.capacity != null)
            .map((vehicle) => vehicle.capacity));
        capacity = selectedVehicle?.capacity ?? maximumCapacity;
    }

    return capacity > 0 && passengers > capacity
        ? `Esta gira necesita más de un viaje para ${passengers} pasajeros.`
        : null;
}
