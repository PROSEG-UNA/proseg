import { describe, expect, it } from 'vitest';
import { describeVehicleChanges, vehicleSavedMessage } from '../vehicleChanges.js';

const BEFORE = { plate: '301-627', type: 'MICROBUS', capacity: 15, available: true };

describe('describeVehicleChanges', () => {
    it('no reporta cambios cuando los datos coinciden', () => {
        expect(describeVehicleChanges(BEFORE, { plate: '301-627', type: 'MICROBUS', capacity: 15, available: true })).toEqual([]);
    });

    it('lista cada campo cambiado en lenguaje natural', () => {
        const changes = describeVehicleChanges(BEFORE, { plate: '301-628', type: null, capacity: 19, available: false });

        expect(changes).toEqual([
            'Placa: de 301-627 a 301-628',
            expect.stringMatching(/^Tipo: se quitó \(antes era /),
            'Capacidad: de 15 a 19',
            'Estado: de Disponible a No disponible',
        ]);
    });

    it('ignora el estado en vehículos prestados', () => {
        expect(describeVehicleChanges({ plate: 'A', type: '', capacity: '' }, { plate: 'A', type: null, capacity: null })).toEqual([]);
    });
});

describe('vehicleSavedMessage', () => {
    it('arma el mensaje de creación, sin cambios y con cambios', () => {
        expect(vehicleSavedMessage({ plate: 'ABC', created: true, changes: [] })).toBe('Vehículo ABC creado correctamente.');
        expect(vehicleSavedMessage({ plate: 'ABC', changes: [] })).toBe('Vehículo ABC guardado. No hubo cambios en sus datos.');
        expect(vehicleSavedMessage({ kind: 'Vehículo prestado', plate: 'ABC', changes: ['Capacidad: de 15 a 19'] }))
            .toBe('Vehículo prestado ABC actualizado correctamente.\n\nCapacidad: de 15 a 19');
    });
});
