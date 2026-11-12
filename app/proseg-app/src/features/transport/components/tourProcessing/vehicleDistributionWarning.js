export function vehicleDistributionWarning(row) {
    if (!row?.needsMultipleTrips) return null;
    return `Esta gira necesita más de un viaje para ${row.passengers} pasajeros.`;
}
