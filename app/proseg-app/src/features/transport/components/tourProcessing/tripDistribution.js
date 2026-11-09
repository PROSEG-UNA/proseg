export const TRIP_MODES = {
    DISTINCT: 'DISTINCT_VEHICLES',
    SAME: 'SAME_DRIVER_AND_VEHICLE',
    SHARED: 'SHARED_DRIVERS',
};

export const TRIP_MODE_LABELS = {
    [TRIP_MODES.DISTINCT]: 'Choferes distintos',
    [TRIP_MODES.SAME]: 'Un chofer y vehículo',
    [TRIP_MODES.SHARED]: 'Choferes que repiten viajes',
};

export const EDITOR_TRIP_MODES = [TRIP_MODES.DISTINCT, TRIP_MODES.SHARED];

export const TRIP_MODE_HINTS = {
    [TRIP_MODES.DISTINCT]: 'Cada viaje lleva su propio chofer y vehículo.',
    [TRIP_MODES.SAME]: 'Completa el chofer y el vehículo del Viaje 1: se repiten en todos los viajes y las horas de la gira se cuentan una sola vez.',
    [TRIP_MODES.SHARED]: 'Indica cuántos choferes hay y completa chofer y vehículo de los primeros viajes, uno por chofer. Con un solo chofer se repite en todos los viajes; con varios, los viajes se alternan entre ellos. Las horas se cuentan una vez por chofer.',
};

const BLANK_RESOURCE = { driver: '', vehicle: '', resourceType: 'INTERNAL', borrowedVehicleId: '', borrowedVehiclePlate: '' };

function resourceOf(assignment) {
    return {
        driver: assignment?.driver ?? '',
        vehicle: assignment?.vehicle ?? '',
        resourceType: assignment?.resourceType || 'INTERNAL',
        borrowedVehicleId: assignment?.borrowedVehicleId ?? '',
        borrowedVehiclePlate: assignment?.borrowedVehiclePlate ?? '',
    };
}

export function minimumTrips(passengers, capacity) {
    const total = Number(passengers);
    const seats = Number(capacity);
    if (!Number.isInteger(total) || total < 1 || !Number.isInteger(seats) || seats < 1) return null;
    return Math.ceil(total / seats);
}

export function distributePassengers(total, capacities) {
    const result = capacities.map(() => 0);
    let remaining = Math.max(0, Number(total) || 0);
    let open = capacities.map((_, index) => index);
    while (remaining > 0 && open.length > 0) {
        const share = Math.floor(remaining / open.length);
        const extra = remaining % open.length;
        let placed = 0;
        const stillOpen = [];
        open.forEach((index, position) => {
            const wanted = share + (position < extra ? 1 : 0);
            const capacity = capacities[index];
            const room = capacity == null ? wanted : capacity - result[index];
            const given = Math.max(0, Math.min(wanted, room));
            result[index] += given;
            placed += given;
            if (capacity == null || result[index] < capacity) stillOpen.push(index);
        });
        if (placed === 0) break;
        remaining -= placed;
        open = stillOpen;
    }
    return result;
}

export function detectTripMode(assignments) {
    const drivers = assignments.map((assignment) => assignment.driver?.trim().toLowerCase()).filter(Boolean);
    const vehicles = assignments
        .map((assignment) => (assignment.resourceType === 'BORROWED_VEHICLE' ? assignment.borrowedVehicleId : assignment.vehicle?.trim().toUpperCase()))
        .filter(Boolean);
    const repeatsDriver = new Set(drivers).size < drivers.length;
    const repeatsVehicle = new Set(vehicles).size < vehicles.length;
    if (!repeatsDriver && !repeatsVehicle) return TRIP_MODES.DISTINCT;
    return TRIP_MODES.SHARED;
}

export function suggestedTripCount({ passengers, capacity, minimum = 2 }) {
    return Math.max(minimum, minimumTrips(passengers, capacity) ?? minimum);
}

export function buildTrips({ mode, passengers, tripCount, driverCount = 2, assignments, capacityOf }) {
    const total = Number(passengers);
    const count = Math.max(1, Math.min(Number(tripCount) || 1, Number.isInteger(total) && total > 0 ? total : 1));
    const first = resourceOf(assignments[0]);
    let resources;
    if (mode === TRIP_MODES.SAME) {
        resources = Array.from({ length: count }, () => ({ ...first }));
    } else if (mode === TRIP_MODES.SHARED) {
        const drivers = Math.max(1, Math.min(Number(driverCount) || 1, count));
        const pool = Array.from({ length: drivers }, (_, index) => (assignments[index] ? resourceOf(assignments[index]) : { ...BLANK_RESOURCE }));
        resources = Array.from({ length: count }, (_, index) => ({ ...pool[index % drivers] }));
    } else {
        resources = Array.from({ length: count }, (_, index) => (index === 0 ? { ...first } : { ...BLANK_RESOURCE }));
    }
    const shares = distributePassengers(total, resources.map((resource) => capacityOf(resource) ?? null));
    return resources.map((resource, index) => ({ ...resource, passengers: String(shares[index]) }));
}
