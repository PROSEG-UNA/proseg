import { describe, expect, it } from 'vitest';
import { vehicleDistributionWarning } from '../vehicleDistributionWarning.js';

describe('vehicleDistributionWarning', () => {
    it('usa la bandera que calcula el servidor', () => {
        expect(vehicleDistributionWarning({ needsMultipleTrips: true, passengers: 40 })).toBe(
            'Esta gira necesita más de un viaje para 40 pasajeros.',
        );
        expect(vehicleDistributionWarning({ needsMultipleTrips: false, passengers: 40 })).toBeNull();
        expect(vehicleDistributionWarning({ passengers: 40 })).toBeNull();
    });
});
