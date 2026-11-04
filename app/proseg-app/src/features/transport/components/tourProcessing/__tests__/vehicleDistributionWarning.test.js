import { describe, expect, it } from 'vitest';
import { vehicleDistributionWarning } from '../vehicleDistributionWarning.js';

const vehicles = [
    { id: '1', label: '301-481', capacity: 30, available: true },
    { id: '2', label: '301-590', capacity: 40, available: true },
];

describe('vehicleDistributionWarning', () => {
    it('avisa cuando los pasajeros superan la capacidad del vehículo elegido', () => {
        expect(vehicleDistributionWarning({ passengers: 500, vehicle: ' 301-481 ' }, vehicles, [])).toBe(
            'Esta gira necesita más de un viaje para 500 pasajeros.',
        );
        expect(vehicleDistributionWarning({ passengers: 30, vehicle: '301-481' }, vehicles, [])).toBeNull();
    });

    it('usa la mayor capacidad disponible cuando aún no hay vehículo asignado', () => {
        expect(vehicleDistributionWarning({ passengers: 50, vehicle: null }, vehicles, [])).toBe(
            'Esta gira necesita más de un viaje para 50 pasajeros.',
        );
        expect(vehicleDistributionWarning({ passengers: 35, vehicle: null }, vehicles, [])).toBeNull();
    });

    it('usa la capacidad del vehículo prestado y omite giras ya distribuidas o externas', () => {
        const borrowed = [{ id: 'prestado-1', capacity: 20 }];
        expect(vehicleDistributionWarning({ passengers: 25, resourceType: 'BORROWED_VEHICLE', borrowedVehicleId: 'prestado-1' }, vehicles, borrowed)).toBe(
            'Esta gira necesita más de un viaje para 25 pasajeros.',
        );
        expect(vehicleDistributionWarning({ passengers: 500, vehicle: '301-481', assignments: [{}, {}] }, vehicles, [])).toBeNull();
        expect(vehicleDistributionWarning({ passengers: 500, resourceType: 'EXTERNAL' }, vehicles, [])).toBeNull();
    });
});
