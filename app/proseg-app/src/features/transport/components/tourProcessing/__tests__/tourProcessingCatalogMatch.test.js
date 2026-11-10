import { describe, expect, it } from 'vitest';
import {
    canAddToCatalog,
    checksDriverCatalog,
    driverMatchKey,
    looksLikePlate,
    missingCatalogMessage,
    plateMatchKey,
} from '../tourProcessingCatalogMatch.js';

describe('checksDriverCatalog', () => {
    it('no advierte el chofer de un prestamo ni de una contratada', () => {
        expect(checksDriverCatalog('BORROWED_VEHICLE')).toBe(false);
        expect(checksDriverCatalog('EXTERNAL')).toBe(false);
        expect(checksDriverCatalog('INTERNAL')).toBe(true);
        expect(checksDriverCatalog(undefined)).toBe(true);
    });
});

describe('driverMatchKey', () => {
    it('ignora mayusculas, espacios sobrantes y valores vacios', () => {
        expect(driverMatchKey('  Jorge   Francisco MENA ')).toBe('jorge francisco mena');
        expect(driverMatchKey('')).toBe('');
        expect(driverMatchKey(null)).toBe('');
    });

    it('distingue acentos igual que el registro de giras', () => {
        expect(driverMatchKey('José')).not.toBe(driverMatchKey('Jose'));
    });
});

describe('plateMatchKey', () => {
    it('ignora mayusculas y espacios pero conserva el guion', () => {
        expect(plateMatchKey(' 301 - 627 ')).toBe('301-627');
        expect(plateMatchKey('bbb 123')).toBe('BBB123');
        expect(plateMatchKey('301-627')).not.toBe(plateMatchKey('301627'));
    });
});

describe('looksLikePlate', () => {
    it('trata como placa solo lo que tiene digitos', () => {
        expect(looksLikePlate('301-627')).toBe(true);
        expect(looksLikePlate('Microbus')).toBe(false);
        expect(looksLikePlate('Pick-Up')).toBe(false);
        expect(looksLikePlate(null)).toBe(false);
    });
});

describe('canAddToCatalog y missingCatalogMessage', () => {
    it('un vehiculo con forma de placa se puede agregar y pide agregarlo', () => {
        expect(canAddToCatalog('vehicle', '999-999')).toBe(true);
        expect(missingCatalogMessage('vehicle', '999-999')).toBe(
            'Este vehículo no se encuentra en el sistema, agrégalo antes de continuar',
        );
    });

    it('un vehiculo sin forma de placa no se puede agregar y avisa que no es una placa', () => {
        expect(canAddToCatalog('vehicle', 'Todo Terreno')).toBe(false);
        expect(missingCatalogMessage('vehicle', 'Todo Terreno')).toBe(
            'Este valor no es una placa, selecciona un vehículo del sistema o deja el campo vacío',
        );
    });

    it('un chofer siempre se puede agregar', () => {
        expect(canAddToCatalog('driver', 'Juan Chacsn')).toBe(true);
        expect(missingCatalogMessage('driver', 'Juan Chacsn')).toBe(
            'Este chofer no se encuentra en el sistema, agrégalo antes de continuar',
        );
    });
});
