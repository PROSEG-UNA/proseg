import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import CleaningDraftRowModal from '../CleaningDraftRowModal.jsx';
import { updateCleaningDraftRow, updateCleaningDraftRowDay } from '../../../services/cleaning/cleaningService.js';

vi.mock('../../../services/cleaning/cleaningService.js', () => ({
    createCleaningDraftRow: vi.fn(),
    deleteCleaningDraftRow: vi.fn(),
    updateCleaningDraftRow: vi.fn(),
    updateCleaningDraftRowDay: vi.fn(),
}));

vi.mock('../../../hooks/useCleaningCatalogMatch.js', () => ({
    useCleaningCatalogMatch: () => ({
        driver: { options: ['Jorge Francisco Mena'], isMissing: (value) => value === 'Juan Chacsn', canAdd: true },
        vehicle: { options: ['301-627'], isMissing: (value) => value === '999-999', canAdd: true },
    }),
}));

const ROW = {
    id: 'row-1',
    rowNumber: 3,
    driver: 'Juan Chacsn',
    number: '04040',
    vehicle: '999-999',
    passengers: 15,
    executingUnit: 'Esc Topografia Catastro Y Geodesia',
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
    duplicateGroupSize: 1,
    duplicateRank: 1,
    conflicting: false,
};

const ROW_WITH_DAYS = {
    ...ROW,
    days: [{
        id: 'day-1',
        date: '2026-05-02',
        dayOfWeek: 'martes',
        startTime: '07:00',
        endTime: '19:00',
        ordinaryHours: 9,
        surplusHours: 3,
        overtimeHours: 0,
        manuallyEdited: false,
    }],
};

function renderModal(mode, row = ROW) {
    render(
        <AppTheme defaultColorScheme="light">
            <QueryClientProvider client={new QueryClient()}>
                <LocalizationProvider dateAdapter={AdapterDayjs}>
                    <CleaningDraftRowModal
                        open
                        mode={mode}
                        row={row}
                        draftId="draft-1"
                        onClose={vi.fn()}
                        onSaved={vi.fn()}
                    />
                </LocalizationProvider>
            </QueryClientProvider>
        </AppTheme>,
    );
}

beforeEach(() => {
    updateCleaningDraftRow.mockReset();
    updateCleaningDraftRow.mockResolvedValue(ROW);
    updateCleaningDraftRowDay.mockReset();
});

afterEach(cleanup);

describe('CleaningDraftRowModal con chofer y vehiculo fuera del sistema', () => {
    it('guarda la fila con el chofer vacio y el vehiculo elegido del sistema', async () => {
        renderModal('edit');

        expect(screen.getByText(/Este chofer no se encuentra en el sistema/)).toBeInTheDocument();
        expect(screen.getByText(/Este vehículo no se encuentra en el sistema/)).toBeInTheDocument();

        const [clearDriver] = screen.getAllByTitle('Clear');
        await userEvent.click(clearDriver);
        expect(screen.getByRole('combobox', { name: 'Chofer' })).toHaveValue('');
        expect(screen.queryByText(/Este chofer no se encuentra en el sistema/)).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Seleccionar otro vehículo' }));
        await userEvent.click(await screen.findByRole('option', { name: '301-627' }));
        expect(screen.queryByText(/Este vehículo no se encuentra en el sistema/)).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateCleaningDraftRow).toHaveBeenCalledTimes(1));
        expect(updateCleaningDraftRow).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            expect.objectContaining({ driver: null, vehicle: '301-627', number: '04040' }),
        );
    });

    it('permite guardar sin resolver las advertencias', async () => {
        renderModal('edit');

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateCleaningDraftRow).toHaveBeenCalledTimes(1));
        expect(updateCleaningDraftRow).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            expect.objectContaining({ driver: 'Juan Chacsn', vehicle: '999-999' }),
        );
    });

    it('en modo ver muestra los iconos de advertencia sin ofrecer opciones', () => {
        renderModal('view');

        expect(screen.getAllByRole('img')).toHaveLength(2);
        expect(screen.queryByRole('button', { name: 'Seleccionar otro chofer' })).not.toBeInTheDocument();
    });
});

describe('CleaningDraftRowModal al cambiar la duracion', () => {
    it('mueve la fecha de regreso segun la duracion y solo la envia al guardar', async () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null });

        const durationField = screen.getByRole('textbox', { name: /Duración/ });
        await userEvent.clear(durationField);
        await userEvent.type(durationField, '3');

        expect(screen.getByRole('group', { name: /Fecha regreso/ })).toHaveTextContent('04/05/2026');
        expect(updateCleaningDraftRow).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateCleaningDraftRow).toHaveBeenCalledTimes(1));
        expect(updateCleaningDraftRow).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            expect.objectContaining({ durationDays: 3, departureDate: '2026-05-02', returnDate: '2026-05-04' }),
        );
    });

    it('vuelve la fecha de regreso al dia de salida cuando la duracion baja a 1', async () => {
        renderModal('edit', { ...ROW, durationDays: 2, returnDate: '2026-05-03', driver: null, vehicle: null });

        const durationField = screen.getByRole('textbox', { name: /Duración/ });
        await userEvent.clear(durationField);
        await userEvent.type(durationField, '1');

        expect(screen.getByRole('group', { name: /Fecha regreso/ })).toHaveTextContent('02/05/2026');
    });
});

describe('CleaningDraftRowModal con fechas de regreso invalidas', () => {
    it('marca la fecha de regreso anterior a la salida sin esperar al guardado y no permite guardar', async () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null, returnDate: '2026-05-01' });

        expect(screen.getByText('La fecha de regreso es anterior a la fecha de salida')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Revisa los datos antes de continuar')).toBeInTheDocument();
        expect(updateCleaningDraftRow).not.toHaveBeenCalled();
    });

    it('marca la hora de regreso anterior a la salida cuando es el mismo dia', async () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null, returnTime: '06:00' });

        expect(screen.getByText('La hora de regreso es anterior a la hora de salida')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Revisa los datos antes de continuar')).toBeInTheDocument();
        expect(updateCleaningDraftRow).not.toHaveBeenCalled();
    });

    it('no marca la hora de regreso anterior cuando la gira termina otro dia', () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null, returnDate: '2026-05-03', returnTime: '06:00' });

        expect(screen.queryByText('La hora de regreso es anterior a la hora de salida')).not.toBeInTheDocument();
    });
});

describe('CleaningDraftRowModal cuando el chofer no puede atender la nueva duracion', () => {
    it('muestra una advertencia con el motivo que devuelve el servidor', async () => {
        updateCleaningDraftRow.mockRejectedValueOnce({
            response: {
                data: {
                    message: 'No se puede aumentar la duración de la gira porque el chofer asignado no puede atenderla.',
                    errors: ['CLEANING_DRAFT_ROW_DRIVER_UNAVAILABLE'],
                },
            },
        });
        renderModal('edit', { ...ROW, driver: null, vehicle: null });

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Chofer no disponible')).toBeInTheDocument();
        expect(screen.getByText(/el chofer asignado no puede atenderla/)).toBeInTheDocument();
    });
});

describe('CleaningDraftRowModal con dias persistidos', () => {
    it('no muestra la seccion de dias cuando la gira no tiene chofer asignado', () => {
        renderModal('edit', ROW);

        expect(screen.queryByText('Días')).not.toBeInTheDocument();
    });

    it('muestra la fecha, el dia de la semana y las horas de cada dia', () => {
        renderModal('view', ROW_WITH_DAYS);

        expect(screen.getByText('02/05/2026 · Martes')).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: 'Hora inicio' })).toHaveValue('07:00');
        expect(screen.getByRole('textbox', { name: 'Hora fin' })).toHaveValue('19:00');
        expect(screen.getByRole('textbox', { name: 'Horas ordinarias' })).toHaveValue('9');
        expect(screen.getByRole('textbox', { name: 'Horas sobresueldo' })).toHaveValue('3');
        expect(screen.getByRole('textbox', { name: 'Horas extra' })).toHaveValue('0');
    });

    it('permite editar manualmente las horas de un dia y las guarda', async () => {
        updateCleaningDraftRowDay.mockResolvedValue({
            id: 'day-1',
            date: '2026-05-02',
            dayOfWeek: 'martes',
            startTime: '07:00',
            endTime: '19:00',
            ordinaryHours: 9,
            surplusHours: 5,
            overtimeHours: 0,
            manuallyEdited: true,
        });
        renderModal('edit', ROW_WITH_DAYS);

        await userEvent.click(screen.getByRole('button', { name: 'Editar día' }));

        const surplusField = screen.getByRole('textbox', { name: 'Horas sobresueldo' });
        await userEvent.clear(surplusField);
        await userEvent.type(surplusField, '5');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar día' }));

        await waitFor(() => expect(updateCleaningDraftRowDay).toHaveBeenCalledTimes(1));
        expect(updateCleaningDraftRowDay).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            'day-1',
            expect.objectContaining({ startTime: '07:00', endTime: '19:00', ordinaryHours: 9, surplusHours: 5, overtimeHours: 0 }),
        );
        expect(await screen.findByText('Editado manualmente')).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: 'Horas sobresueldo' })).toHaveValue('5');
    });

    it('bloquea del lado del cliente mas de 9 horas ordinarias en un dia', async () => {
        renderModal('edit', ROW_WITH_DAYS);

        await userEvent.click(screen.getByRole('button', { name: 'Editar día' }));
        const ordinaryField = screen.getByRole('textbox', { name: 'Horas ordinarias' });
        await userEvent.clear(ordinaryField);
        await userEvent.type(ordinaryField, '10');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar día' }));

        expect(await screen.findByText('No puede superar las 9 horas')).toBeInTheDocument();
        expect(updateCleaningDraftRowDay).not.toHaveBeenCalled();
    });

    it('pide confirmacion para horas extra por debajo del 95% de sobresueldo y reintenta al confirmar', async () => {
        updateCleaningDraftRowDay.mockRejectedValueOnce({
            response: { data: { errors: ['CLEANING_DRAFT_ROW_DAY_OVERTIME_NEEDS_CONFIRMATION'] } },
        });
        updateCleaningDraftRowDay.mockResolvedValueOnce({
            id: 'day-1',
            date: '2026-05-02',
            dayOfWeek: 'martes',
            startTime: '07:00',
            endTime: '19:00',
            ordinaryHours: 9,
            surplusHours: 0,
            overtimeHours: 2,
            manuallyEdited: true,
        });
        renderModal('edit', ROW_WITH_DAYS);

        await userEvent.click(screen.getByRole('button', { name: 'Editar día' }));
        const overtimeField = screen.getByRole('textbox', { name: 'Horas extra' });
        await userEvent.clear(overtimeField);
        await userEvent.type(overtimeField, '2');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar día' }));

        expect(await screen.findByText(/no alcanza el 95%/)).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Sí, asignar de todas formas' }));

        await waitFor(() => expect(updateCleaningDraftRowDay).toHaveBeenCalledTimes(2));
        expect(updateCleaningDraftRowDay).toHaveBeenLastCalledWith(
            'draft-1',
            'row-1',
            'day-1',
            expect.objectContaining({ overtimeHours: 2, confirmOvertimeBelowSurplusThreshold: true }),
        );
        expect(await screen.findByText('Editado manualmente')).toBeInTheDocument();
    });
});
