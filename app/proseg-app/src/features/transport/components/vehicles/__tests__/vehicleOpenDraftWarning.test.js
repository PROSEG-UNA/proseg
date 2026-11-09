import { describe, expect, it } from 'vitest';
import { formatVehicleUnavailableWarning } from '../vehicleOpenDraftWarning.js';

describe('formatVehicleUnavailableWarning', () => {
    it('lista las giras del borrador sin decir que se desasignaron', () => {
        const message = formatVehicleUnavailableWarning([
            { year: 2026, month: 5, number: '05100', departureDate: '2026-05-04' },
            { fileName: 'junio.xlsx', number: '05200' },
        ]);

        expect(message).toContain('2 giras asignadas');
        expect(message).toContain('Cámbialas a mano');
        expect(message).toContain('Mayo 2026, gira 05100, 04/05/2026');
        expect(message).toContain('junio.xlsx, gira 05200');
        expect(message).not.toContain('desasign');
    });
});
