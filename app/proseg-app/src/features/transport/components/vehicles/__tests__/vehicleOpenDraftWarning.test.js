import { describe, expect, it } from 'vitest';
import {
    OPEN_DRAFT_TOURS_ACTION,
    formatVehicleOpenDraftResult,
    formatVehicleUnavailablePrompt,
} from '../vehicleOpenDraftWarning.js';

describe('formatVehicleUnavailablePrompt', () => {
    it('lista las giras y explica las tres opciones', () => {
        const message = formatVehicleUnavailablePrompt('301-295', [
            { year: 2026, month: 5, number: '05100', departureDate: '2026-05-04' },
            { fileName: 'junio.xlsx', number: '05200' },
        ]);

        expect(message).toContain('El vehículo 301-295 tiene 2 giras asignadas');
        expect(message).toContain('Mayo 2026, gira 05100, 04/05/2026');
        expect(message).toContain('junio.xlsx, gira 05200');
        expect(message).toContain('Eliminar apariciones');
        expect(message).toContain('Mantener');
        expect(message).toContain('Cancelar');
    });

    it('usa singular con una sola gira', () => {
        const message = formatVehicleUnavailablePrompt('301-295', [{ number: '03612' }]);

        expect(message).toContain('1 gira asignada');
    });
});

describe('formatVehicleOpenDraftResult', () => {
    it('informa cuando se eliminó el vehículo de las giras', () => {
        expect(formatVehicleOpenDraftResult(OPEN_DRAFT_TOURS_ACTION.remove, 2)).toBe('Se eliminó el vehículo de 2 giras en borradores abiertos.');
    });

    it('informa cuando se mantuvo con advertencia', () => {
        expect(formatVehicleOpenDraftResult(OPEN_DRAFT_TOURS_ACTION.keep, 1)).toBe('El vehículo se mantiene en 1 gira de borradores abiertos, con una advertencia.');
    });
});
