import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { useTourProcessingCatalogMatch } from '../useTourProcessingCatalogMatch.js';

const mocks = vi.hoisted(() => ({
    permissions: new Set(),
    drivers: { options: [], loading: false, error: null },
    vehicles: { options: [], loading: false, error: null },
}));

vi.mock('../../../../common/hooks/index.js', () => ({
    usePermissions: () => ({
        hasPermission: (permission) => mocks.permissions.has(permission),
        hasAnyPermission: (list) => list.some((permission) => mocks.permissions.has(permission)),
    }),
}));

vi.mock('../useTransportOptions.js', () => ({
    useDriverOptions: () => mocks.drivers,
    useVehicleOptions: () => mocks.vehicles,
}));

beforeEach(() => {
    mocks.permissions = new Set([
        PERMISSIONS.TRANSPORT.DRIVERS.READ,
        PERMISSIONS.TRANSPORT.VEHICLES.READ,
    ]);
    mocks.drivers = {
        options: [{ id: '1', label: 'Jorge Francisco Mena' }, { id: '2', label: 'Miguel Fernandez' }],
        loading: false,
        error: null,
    };
    mocks.vehicles = {
        options: [{ id: '1', label: '301-627' }],
        loading: false,
        error: null,
    };
});

function renderCatalog() {
    return renderHook(() => useTourProcessingCatalogMatch()).result.current;
}

describe('useTourProcessingCatalogMatch', () => {
    it('marca como faltante el chofer que no esta en el sistema', () => {
        const { driver } = renderCatalog();

        expect(driver.isMissing('Juan Chacsn')).toBe(true);
        expect(driver.isMissing('jorge  francisco MENA')).toBe(false);
        expect(driver.isMissing('')).toBe(false);
        expect(driver.isMissing(null)).toBe(false);
    });

    it('marca como faltante solo las placas que no estan en el sistema', () => {
        const { vehicle } = renderCatalog();

        expect(vehicle.isMissing('999-999')).toBe(true);
        expect(vehicle.isMissing('301-627')).toBe(false);
        expect(vehicle.isMissing(' 301-627 ')).toBe(false);
    });

    it('marca como faltante cualquier valor que no sea una placa del sistema, incluidos los tipos', () => {
        const { vehicle } = renderCatalog();

        expect(vehicle.isMissing('Microbus')).toBe(true);
        expect(vehicle.isMissing('Todo Terreno')).toBe(true);
        expect(vehicle.isMissing('Pick-Up')).toBe(true);
        expect(vehicle.isMissing('')).toBe(false);
        expect(vehicle.isMissing(null)).toBe(false);
    });

    it('no marca nada mientras el catalogo carga o falla', () => {
        mocks.drivers = { options: [], loading: true, error: null };
        mocks.vehicles = { options: [], loading: false, error: new Error('403') };
        const { driver, vehicle } = renderCatalog();

        expect(driver.isMissing('Juan Chacsn')).toBe(false);
        expect(vehicle.isMissing('999-999')).toBe(false);
    });

    it('no marca nada sin permiso para leer el catalogo', () => {
        mocks.permissions = new Set();
        const { driver, vehicle } = renderCatalog();

        expect(driver.isMissing('Juan Chacsn')).toBe(false);
        expect(vehicle.isMissing('999-999')).toBe(false);
    });

    it('arma las opciones con los datos de cada chofer y vehiculo', () => {
        mocks.drivers.options = [{
            id: '1', label: 'Jorge Francisco Mena', status: 'INACTIVE', unit: 'OTHER_UNIT',
            ordinaryStart: '07:30:00', ordinaryEnd: '16:30:00', allowedVehicleTypes: ['MICROBUS'], restrictions: '',
        }];
        mocks.vehicles.options = [{ id: '1', label: '301-627', type: 'MICROBUS', capacity: 36, available: false }];
        const { driver, vehicle } = renderCatalog();

        expect(driver.options[0]).toMatchObject({
            label: 'Jorge Francisco Mena',
            unit: 'OTHER_UNIT',
            secondary: 'Otra Unidad · 07:30 a 16:30 · Maneja: Microbús',
            badge: { label: 'Inactivo', color: 'error' },
        });
        expect(vehicle.options[0]).toMatchObject({
            label: '301-627',
            secondary: 'Microbús · 36 pasajeros',
            badge: { label: 'No disponible', color: 'error' },
        });
        expect(vehicle.options[0].searchText).toContain('36 pasajeros');
    });

    it('solo permite agregar con el permiso de gestionar', () => {
        expect(renderCatalog().driver.canAdd).toBe(false);

        mocks.permissions.add(PERMISSIONS.TRANSPORT.DRIVERS.MANAGE);
        const { driver, vehicle } = renderCatalog();
        expect(driver.canAdd).toBe(true);
        expect(vehicle.canAdd).toBe(false);
    });
});
