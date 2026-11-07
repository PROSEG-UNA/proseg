import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingDriverAssignmentReport from '../TourProcessingDriverAssignmentReport.jsx';

afterEach(cleanup);

const renderReport = (props) => render(
    <AppTheme defaultColorScheme="light">
        <TourProcessingDriverAssignmentReport open onClose={() => {}} {...props} />
    </AppTheme>,
);

const fullResult = {
    assignedTours: 20,
    unassignedTours: 2,
    surplusTargetHours: 35.625,
    surplusDeficits: { Ana: 2.125, Beto: 10 },
    surplusTargets: { Ana: 18.05 },
    unassignedDetails: [
        { rowId: '1', number: '03840', reasons: ['Ana tiene un traslape con la gira 02787.'] },
        { rowId: '3', number: '04461', reasons: ['No hay chofer con descanso suficiente.'] },
    ],
    preservedAssignmentWarnings: [{ rowId: '2', number: '01234', reasons: ['El chofer Beto está inactivo.'] }],
    optimizationLimitReached: true,
};

describe('TourProcessingDriverAssignmentReport', () => {
    it('muestra los contadores y las giras pendientes con sus motivos', () => {
        renderReport({ result: fullResult });

        expect(screen.getByText('Resultado de la autoasignación')).toBeTruthy();
        expect(screen.getByText('Asignadas')).toBeTruthy();
        expect(screen.getByText('20')).toBeTruthy();
        expect(screen.getByText(/Se alcanzó el límite de búsqueda/)).toBeTruthy();
        expect(screen.getByRole('tab', { name: 'Giras pendientes (2)' })).toBeTruthy();
        expect(screen.getByText('03840')).toBeTruthy();
        expect(screen.getByText('Ana tiene un traslape con la gira 02787.')).toBeTruthy();
    });

    it('cambia entre pendientes, conservadas que requieren revisión y déficit de sobresueldo', () => {
        renderReport({ result: fullResult });

        fireEvent.click(screen.getByRole('tab', { name: 'Requieren revisión (1)' }));
        expect(screen.getByText('El chofer Beto está inactivo.')).toBeTruthy();
        expect(screen.queryByText('03840')).toBeNull();

        fireEvent.click(screen.getByRole('tab', { name: 'Sobresueldo (2)' }));
        expect(screen.getByText('Ana')).toBeTruthy();
        expect(screen.getByText(/Sobresueldo pendiente para alcanzar el 95%/)).toBeTruthy();
    });

    it('muestra la meta del 95% de cada chofer y usa la del mes si no viene la individual', () => {
        renderReport({ result: fullResult });

        fireEvent.click(screen.getByRole('tab', { name: 'Sobresueldo (2)' }));
        const anaRow = screen.getByText('Ana').closest('tr');
        const betoRow = screen.getByText('Beto').closest('tr');

        expect(anaRow.textContent).toContain(`${(18.05).toLocaleString('es-CR', { maximumFractionDigits: 3 })} h`);
        expect(anaRow.textContent).toContain(`${(2.125).toLocaleString('es-CR', { maximumFractionDigits: 3 })} h`);
        expect(betoRow.textContent).toContain(`${(35.625).toLocaleString('es-CR', { maximumFractionDigits: 3 })} h`);
    });

    it('muestra el resumen de las obligatorias con cubridores, advertencias y giras sin atender', () => {
        renderReport({
            result: {
                assignedTours: 30,
                unassignedTours: 1,
                priorityOneUnassigned: 4,
                mandatoryCoverage: {
                    total: 12,
                    fulfilled: 9,
                    withWarning: 2,
                    unattended: 1,
                    warnings: [{ rowId: 'w1', number: '09444', reasons: ['Asignada a Ana con sobresueldo al 65% (26 de 40 horas).'] }],
                    unattendedDetails: [{ rowId: 'u1', number: '09790', reasons: ['Ningún chofer puede tomarla: 3 por cruce de horario.'] }],
                    coveringDrivers: [
                        { name: 'Ana', surplusHours: 26, targetHours: 38, surplusPercent: 65, tourNumbers: ['09444', '09783'] },
                        { name: 'Beto', surplusHours: 40, targetHours: 38, surplusPercent: 100, tourNumbers: ['09785'] },
                    ],
                    driversBelowTarget: 5,
                    driversBelowTargetByConcentration: 3,
                },
            },
        });

        expect(screen.getByText('Prioridad 1 sin atender')).toBeTruthy();
        fireEvent.click(screen.getByRole('tab', { name: 'Obligatorias (12)' }));
        expect(screen.getByText('Con 95 % cumplido')).toBeTruthy();
        expect(screen.getByText('Con advertencia')).toBeTruthy();
        expect(screen.getByText('Sin atender')).toBeTruthy();
        expect(screen.getByText(/Choferes por debajo del 95 %: 5\. De ellos, 3 quedaron así porque el sobresueldo se concentró en los cubridores\./)).toBeTruthy();
        expect(screen.getByText('Giras de prioridad 1 sin atender en total: 4.')).toBeTruthy();
        const anaRow = screen.getByText('Ana').closest('tr');
        expect(anaRow.textContent).toContain('09444, 09783');
        expect(anaRow.textContent).toContain('65 %');
        expect(screen.getByText('Asignada a Ana con sobresueldo al 65% (26 de 40 horas).')).toBeTruthy();
        expect(screen.getByText('Ningún chofer puede tomarla: 3 por cruce de horario.')).toBeTruthy();
    });

    it('no muestra la pestaña de obligatorias cuando el resultado no trae resumen', () => {
        renderReport({ result: fullResult });

        expect(screen.queryByRole('tab', { name: /Obligatorias/ })).toBeNull();
        expect(screen.queryByText('Prioridad 1 sin atender')).toBeNull();
    });

    it('filtra las giras por número o motivo', () => {
        renderReport({ result: fullResult });

        fireEvent.change(screen.getByPlaceholderText(/Buscar por número de gira o motivo/), { target: { value: 'descanso' } });
        expect(screen.getByText('04461')).toBeTruthy();
        expect(screen.queryByText('03840')).toBeNull();

        fireEvent.change(screen.getByPlaceholderText(/Buscar por número de gira o motivo/), { target: { value: 'zzz' } });
        expect(screen.getByText('Ninguna gira coincide con la búsqueda.')).toBeTruthy();
    });

    it('permite cerrar el resultado y admite respuestas sin detalles', () => {
        const close = vi.fn();
        renderReport({ result: { assignedTours: 2, unassignedTours: 0 }, onClose: close });

        expect(screen.getByText('Todas las giras se asignaron sin observaciones.')).toBeTruthy();
        expect(screen.getByRole('tab', { name: 'Choferes' })).toBeEnabled();
        expect(screen.getByRole('tab', { name: 'Vehículos' })).toBeDisabled();
        fireEvent.click(screen.getByRole('button', { name: 'Cerrar' }));
        expect(close).toHaveBeenCalledOnce();
    });

    it('muestra los motivos de vehículos en otra pestaña del mismo resultado', () => {
        renderReport({
            result: fullResult,
            vehicleResult: {
                assignedTours: 3,
                unassignedTours: 1,
                unassignedDetails: [{ rowId: 'v1', number: '05100', reasons: ['No hay vehículo con capacidad suficiente.'] }],
                preservedAssignmentWarnings: [{ rowId: 'v2', number: '05200', reasons: ['El vehículo no tiene capacidad registrada.'] }],
            },
        });

        fireEvent.click(screen.getByRole('tab', { name: 'Vehículos' }));
        expect(screen.getByText('No hay vehículo con capacidad suficiente.')).toBeTruthy();
        fireEvent.click(screen.getByRole('tab', { name: 'Requieren revisión (1)' }));
        expect(screen.getByText('El vehículo no tiene capacidad registrada.')).toBeTruthy();
        fireEvent.click(screen.getByRole('tab', { name: 'Choferes' }));
        expect(screen.getByText('Ana tiene un traslape con la gira 02787.')).toBeTruthy();
    });

    it('abre directamente vehículos cuando solo se autoasignaron vehículos', () => {
        renderReport({ result: null, vehicleResult: { assignedTours: 0, unassignedTours: 0 } });

        expect(screen.getByRole('tab', { name: 'Choferes' })).toBeDisabled();
        expect(screen.getByRole('tab', { name: 'Vehículos' })).toBeEnabled();
        expect(screen.getByText('Todas las giras se asignaron sin observaciones.')).toBeTruthy();
    });

    it('no renderiza nada sin resultado', () => {
        renderReport({ result: null });

        expect(screen.queryByText('Resultado de la autoasignación')).toBeNull();
    });
});
