import { describe, expect, it } from 'vitest';
import {
    TRIP_MODES, buildTrips, detectTripMode, distributePassengers, minimumTrips, suggestedTripCount,
} from '../tripDistribution.js';

const withCapacity = (capacity) => () => capacity;
const blank = { driver: '', vehicle: '', resourceType: 'INTERNAL', borrowedVehicleId: '' };

describe('minimumTrips', () => {
    it('calcula el mínimo de viajes según la capacidad del vehículo', () => {
        expect(minimumTrips(150, 30)).toBe(5);
        expect(minimumTrips(151, 30)).toBe(6);
        expect(minimumTrips(500, 45)).toBe(12);
    });

    it('no sugiere nada cuando la capacidad o los pasajeros no se conocen', () => {
        expect(minimumTrips(50, undefined)).toBeNull();
        expect(minimumTrips(50, 0)).toBeNull();
        expect(minimumTrips('', 30)).toBeNull();
    });

    it('sugiere al menos dos viajes al dividir una gira', () => {
        expect(suggestedTripCount({ passengers: 20, capacity: 30 })).toBe(2);
        expect(suggestedTripCount({ passengers: 50, capacity: 30 })).toBe(2);
        expect(suggestedTripCount({ passengers: 150, capacity: 30 })).toBe(5);
    });
});

describe('distributePassengers', () => {
    it('reparte de forma equitativa', () => {
        expect(distributePassengers(150, [30, 30, 30, 30, 30])).toEqual([30, 30, 30, 30, 30]);
        expect(distributePassengers(50, [30, 30])).toEqual([25, 25]);
        expect(distributePassengers(70, [null, null, null])).toEqual([24, 23, 23]);
    });

    it('respeta la capacidad de cada vehículo y pasa el resto a los que tienen espacio', () => {
        expect(distributePassengers(40, [30, 10])).toEqual([30, 10]);
        expect(distributePassengers(20, [30, 10])).toEqual([10, 10]);
    });

    it('deja pasajeros sin cubrir cuando no caben en los vehículos', () => {
        expect(distributePassengers(100, [30, 30])).toEqual([30, 30]);
    });
});

describe('buildTrips', () => {
    it('manera 1: repite chofer y vehículo del primer viaje en todos los viajes', () => {
        const trips = buildTrips({
            mode: TRIP_MODES.SAME,
            passengers: 150,
            tripCount: 5,
            assignments: [{ ...blank, driver: 'Ana', vehicle: '301-1', passengers: '75' }, { ...blank, passengers: '75' }],
            capacityOf: withCapacity(30),
        });

        expect(trips).toHaveLength(5);
        expect(trips.map((trip) => trip.passengers)).toEqual(['30', '30', '30', '30', '30']);
        expect(trips.every((trip) => trip.driver === 'Ana' && trip.vehicle === '301-1')).toBe(true);
    });

    it('variante 1b: alterna los viajes entre los choferes definidos', () => {
        const trips = buildTrips({
            mode: TRIP_MODES.SHARED,
            passengers: 180,
            tripCount: 6,
            driverCount: 2,
            assignments: [{ ...blank, driver: 'Ana', vehicle: '301-1' }, { ...blank, driver: 'Beto', vehicle: '301-2' }],
            capacityOf: withCapacity(30),
        });

        expect(trips.map((trip) => trip.driver)).toEqual(['Ana', 'Beto', 'Ana', 'Beto', 'Ana', 'Beto']);
        expect(trips.map((trip) => trip.vehicle)).toEqual(['301-1', '301-2', '301-1', '301-2', '301-1', '301-2']);
        expect(trips.map((trip) => trip.passengers)).toEqual(['30', '30', '30', '30', '30', '30']);
    });

    it('permite tres choferes que se reparten los viajes', () => {
        const trips = buildTrips({
            mode: TRIP_MODES.SHARED,
            passengers: 600,
            tripCount: 12,
            driverCount: 3,
            assignments: [{ ...blank, driver: 'Ana' }, { ...blank, driver: 'Beto' }, { ...blank, driver: 'Caro' }],
            capacityOf: () => null,
        });

        expect(trips.map((trip) => trip.driver).slice(0, 4)).toEqual(['Ana', 'Beto', 'Caro', 'Ana']);
        expect(trips.every((trip) => trip.passengers === '50')).toBe(true);
    });

    it('vehículos distintos: conserva solo el primer viaje y deja los demás sin asignar', () => {
        const trips = buildTrips({
            mode: TRIP_MODES.DISTINCT,
            passengers: 70,
            tripCount: 3,
            assignments: [{ ...blank, driver: 'Ana', vehicle: '301-1' }, { ...blank }],
            capacityOf: (resource) => (resource.vehicle ? 30 : null),
        });

        expect(trips.map((trip) => trip.driver)).toEqual(['Ana', '', '']);
        expect(trips.map((trip) => trip.passengers)).toEqual(['24', '23', '23']);
    });

    it('no crea más viajes que pasajeros', () => {
        const trips = buildTrips({
            mode: TRIP_MODES.SAME, passengers: 3, tripCount: 10, assignments: [blank], capacityOf: () => null,
        });

        expect(trips).toHaveLength(3);
    });
});

describe('detectTripMode', () => {
    it('reconoce la manera con la que quedó repartida una gira', () => {
        expect(detectTripMode([{ driver: 'Ana', vehicle: '301-1' }, { driver: 'Beto', vehicle: '301-2' }])).toBe(TRIP_MODES.DISTINCT);
        expect(detectTripMode([{ driver: 'Ana', vehicle: '301-1' }, { driver: 'Ana', vehicle: '301-1' }])).toBe(TRIP_MODES.SAME);
        expect(detectTripMode([
            { driver: 'Ana', vehicle: '301-1' }, { driver: 'Beto', vehicle: '301-2' }, { driver: 'Ana', vehicle: '301-1' },
        ])).toBe(TRIP_MODES.SHARED);
    });
});
