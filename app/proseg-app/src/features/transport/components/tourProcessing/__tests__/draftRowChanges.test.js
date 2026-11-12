import { describe, expect, it } from 'vitest';
import { describeDraftRowChanges } from '../draftRowChanges.js';

const ROW = {
    number: '04040',
    driver: 'Jorge Francisco Mena',
    vehicle: '301-627',
    resourceType: 'INTERNAL',
    passengers: 15,
    executingUnit: 'Escuela',
    responsible: 'Erick Ovares',
    destination: 'Santa Ana',
    durationDays: 1,
    priority: 1,
    modality: 'TPG',
    departureDate: '2026-05-02',
    departureTime: '07:00',
    returnDate: '2026-05-02',
    returnTime: '19:00',
    observations: null,
};

const PAYLOAD = {
    ...ROW,
    driver: 'Jorge Francisco Mena',
    modality: 'TPG',
    observations: null,
    approvalStatus: 'APPROVED',
    assignments: null,
};

describe('describeDraftRowChanges', () => {
    it('no reporta cambios cuando el payload coincide con la fila', () => {
        expect(describeDraftRowChanges(ROW, { ...PAYLOAD, departureTime: '07:00' })).toEqual([]);
    });

    it('lista cada campo cambiado con su valor anterior y el nuevo', () => {
        const changes = describeDraftRowChanges(ROW, {
            ...PAYLOAD,
            driver: 'Daniel Zuniga',
            passengers: 20,
            departureDate: '2026-05-03',
            observations: 'Salida temprano',
        });

        expect(changes).toEqual([
            'Pasajeros: de 15 a 20',
            'Fecha salida: de 02/05/2026 a 03/05/2026',
            'Observaciones: se agregó Salida temprano',
            'Chofer: de Jorge Francisco Mena a Daniel Zuniga',
        ]);
    });

    it('avisa cuando se quita un valor', () => {
        expect(describeDraftRowChanges({ ...ROW, modality: 'TPG' }, { ...PAYLOAD, modality: null }))
            .toEqual(['Modalidad: se quitó (antes era TPG)']);
    });

    it('reporta el cambio de estado de aprobación', () => {
        expect(describeDraftRowChanges(ROW, { ...PAYLOAD, approvalStatus: 'NOT_APPROVED' }))
            .toEqual(['Estado: de Aprobada a No aprobada']);
    });

    it('resume los viajes en vez de comparar chofer y vehículo sueltos', () => {
        const trips = [
            { driver: 'A', vehicle: '1', resourceType: 'INTERNAL', passengers: 8 },
            { driver: 'B', vehicle: '2', resourceType: 'INTERNAL', passengers: 7 },
        ];
        const row = { ...ROW, assignments: trips };
        const sameTrips = describeDraftRowChanges(row, { ...PAYLOAD, driver: 'A', assignments: trips });
        const changedTrips = describeDraftRowChanges(row, {
            ...PAYLOAD,
            assignments: [{ ...trips[0], driver: 'C' }, trips[1]],
        });

        const splitTrips = describeDraftRowChanges(ROW, { ...PAYLOAD, assignments: [
            { driver: 'A', vehicle: '1', resourceType: 'INTERNAL', passengers: 8 },
            { driver: '', vehicle: '', resourceType: 'INTERNAL', passengers: 1 },
        ] });

        expect(sameTrips).toEqual([]);
        expect(changedTrips).toEqual([
            'Viajes: se modificaron',
            'Viaje 1: C, 1, 8 pasajeros',
            'Viaje 2: B, 2, 7 pasajeros',
        ]);
        expect(splitTrips).toEqual([
            'Viajes: ahora son 2 (antes era un solo viaje)',
            'Viaje 1: A, 1, 8 pasajeros',
            'Viaje 2: sin chofer, sin vehículo, 1 pasajero',
        ]);
    });
});
