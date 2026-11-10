import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingFinalCommissionDialog from '../TourProcessingFinalCommissionDialog.jsx';
import { hasFinalCommissionFindings } from '../finalCommissionIssues.js';

afterEach(cleanup);

const renderDialog = (props) => render(
    <AppTheme defaultColorScheme="light">
        <TourProcessingFinalCommissionDialog open subtitle="Mayo 26" onClose={() => {}} onOpenTour={() => {}} onRejectIncomplete={() => {}} {...props} />
    </AppTheme>,
);

const findingsCheck = {
    blockers: [],
    errors: [{
        code: 'OVERSIZED',
        title: 'Giras que necesitan más de un viaje y no están divididas',
        details: ['Gira 100 (04/05/2026): 40 pasajeros para una capacidad de 30'],
        tours: [{ id: 'row-1', number: '100', label: 'Gira 100 (04/05/2026): 40 pasajeros para una capacidad de 30' }],
    }],
    warnings: [{
        code: 'PLATE_REPEATED_SAME_DAY',
        title: 'Placas repetidas el mismo día en giras distintas',
        details: ['Placa 301: giras 100 y 200 (04/05/2026)'],
        tours: [
            { id: 'row-1', number: '100', label: 'Placa 301: giras 100 y 200 (04/05/2026)' },
            { id: 'row-2', number: '200', label: 'Placa 301: giras 100 y 200 (04/05/2026)' },
        ],
    }, {
        code: 'OVERTIME_CAP_EXCEEDED',
        title: 'Choferes que superan las 60 horas extra del mes',
        details: ['Ana: 64 h extra, tope de 60 h'],
        tours: [{ id: 'row-2', number: '200', label: 'Ana: 64 h extra, tope de 60 h' }],
    }],
    rejections: {
        code: 'NOT_APPROVED',
        title: 'Giras sin chofer o sin vehículo',
        details: ['Gira 200 (05/05/2026), prioridad 1, 3 pasajeros: sin vehículo'],
        tours: [{ id: 'row-2', number: '200', label: 'Gira 200 (05/05/2026), prioridad 1, 3 pasajeros: sin vehículo' }],
    },
};

describe('TourProcessingFinalCommissionDialog', () => {
    it('muestra los grupos con cantidad y abre la gira desde el enlace', () => {
        const onOpenTour = vi.fn();
        renderDialog({ check: findingsCheck, onOpenTour });

        expect(screen.getByText('Giras que necesitan más de un viaje (1)')).toBeTruthy();
        expect(screen.getByText('Placa repetida el mismo día (2)')).toBeTruthy();
        expect(screen.getByText('Giras sin atender (1)')).toBeTruthy();
        expect(screen.getByText('Reglas que se están rompiendo (1)')).toBeTruthy();
        expect(screen.queryByRole('checkbox')).toBeNull();

        fireEvent.click(screen.getByRole('button', { name: 'Gira 100 (04/05/2026): 40 pasajeros para una capacidad de 30' }));
        expect(onOpenTour).toHaveBeenCalledWith('row-1');

        fireEvent.click(screen.getByRole('button', { name: 'Gira 200', exact: true }));
        expect(onOpenTour).toHaveBeenCalledWith('row-2');
    });

    it('pide confirmación para pasar las giras sin chofer o vehículo a No aprobada', () => {
        const onRejectIncomplete = vi.fn();
        renderDialog({ check: findingsCheck, onRejectIncomplete });

        fireEvent.click(screen.getByRole('button', { name: 'Pasar las giras sin chofer o vehículo a No aprobada' }));
        expect(onRejectIncomplete).toHaveBeenCalledOnce();
    });

    it('no ofrece pasar a No aprobada si el usuario no puede editar', () => {
        renderDialog({ check: findingsCheck, canRejectIncomplete: false });

        expect(screen.getByText('Giras sin atender (1)')).toBeTruthy();
        expect(screen.queryByRole('button', { name: 'Pasar las giras sin chofer o vehículo a No aprobada' })).toBeNull();
    });

    it('detecta si la revisión no tiene hallazgos', () => {
        expect(hasFinalCommissionFindings({ blockers: [], errors: [], warnings: [], rejections: null })).toBe(false);
        expect(hasFinalCommissionFindings(findingsCheck)).toBe(true);
    });
});
