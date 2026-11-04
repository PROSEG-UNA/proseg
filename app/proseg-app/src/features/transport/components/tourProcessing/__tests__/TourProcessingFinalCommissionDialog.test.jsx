import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingFinalCommissionDialog from '../TourProcessingFinalCommissionDialog.jsx';
import { hasFinalCommissionFindings } from '../finalCommissionIssues.js';

afterEach(cleanup);

const renderDialog = (props) => render(
    <AppTheme defaultColorScheme="light">
        <TourProcessingFinalCommissionDialog open subtitle="Comisión Final Mayo 26" onClose={() => {}} onConfirm={() => {}} {...props} />
    </AppTheme>,
);

const findingsCheck = {
    blockers: [],
    errors: [{ code: 'NO_VEHICLES', title: 'No hay vehículos asignados', details: ['Ninguna gira aprobada tiene un vehículo del catálogo asignado.'] }],
    warnings: [{ code: 'OVERTIME_CAP_EXCEEDED', title: 'Choferes que superan las 60 horas extra del mes', details: ['Ana: 64 h extra, tope de 60 h'] }],
    rejections: { code: 'NOT_APPROVED', title: 'Giras sin chofer o sin vehículo que cambiarán a estado No aprobada', details: ['Gira 03625 (02/05/2026), prioridad 1, 3 pasajeros: sin vehículo'] },
};

describe('TourProcessingFinalCommissionDialog', () => {
    it('habilita la descarga solo cuando se confirman todos los puntos', () => {
        const onConfirm = vi.fn();
        renderDialog({ check: findingsCheck, onConfirm });

        const download = screen.getByRole('button', { name: 'Descargar de todos modos' });
        expect(download.disabled).toBe(true);
        expect(screen.getByText('Ana: 64 h extra, tope de 60 h')).toBeTruthy();

        fireEvent.click(screen.getByLabelText('Entiendo este error y quiero descargar de todos modos'));
        fireEvent.click(screen.getByLabelText('Confirmo que se está rompiendo esta regla'));
        expect(download.disabled).toBe(true);

        fireEvent.click(screen.getByLabelText('Entiendo que estas giras cambiarán a estado No aprobada'));
        expect(download.disabled).toBe(false);

        fireEvent.click(download);
        expect(onConfirm).toHaveBeenCalledWith({ rejectIncomplete: true });
    });

    it('no ofrece la descarga cuando hay giras divididas con viajes incompletos', () => {
        renderDialog({
            check: {
                ...findingsCheck,
                blockers: [{ code: 'PARTIAL_DISTRIBUTION', title: 'Giras divididas en varios viajes', details: ['Gira 03482 (04/05/2026): viajes sin chofer o vehículo: 2 de 3'] }],
            },
        });

        expect(screen.getByText(/No se puede descargar la Comisión Final/)).toBeTruthy();
        expect(screen.getByText('Gira 03482 (04/05/2026): viajes sin chofer o vehículo: 2 de 3')).toBeTruthy();
        expect(screen.queryByRole('button', { name: 'Descargar de todos modos' })).toBeNull();
        expect(screen.queryByText('No hay vehículos asignados')).toBeNull();
    });

    it('no ofrece la descarga si hay giras incompletas y el usuario no puede editarlas', () => {
        renderDialog({ check: findingsCheck, canRejectIncomplete: false });

        expect(screen.getByText(/necesitas permiso para editar giras/)).toBeTruthy();
        expect(screen.getByText('Gira 03625 (02/05/2026), prioridad 1, 3 pasajeros: sin vehículo')).toBeTruthy();
        expect(screen.queryByRole('button', { name: 'Descargar de todos modos' })).toBeNull();
    });

    it('detecta si la revisión no tiene hallazgos', () => {
        expect(hasFinalCommissionFindings({ blockers: [], errors: [], warnings: [], rejections: null })).toBe(false);
        expect(hasFinalCommissionFindings(findingsCheck)).toBe(true);
    });
});
