import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider, QueryObserver } from '@tanstack/react-query';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingDraftRowModal from '../TourProcessingDraftRowModal.jsx';
import { queryKeys } from '../../../../../common/query';
import {
    previewTourProcessingDraftRowDayWarnings,
    previewTourProcessingDraftRowWarnings,
    updateTourProcessingDraftRow,
    updateTourProcessingDraftRowDay,
} from '../../../services/tourProcessing/tourProcessingService.js';

vi.mock('../../../services/tourProcessing/tourProcessingService.js', () => ({
    createTourProcessingDraftRow: vi.fn(),
    deleteTourProcessingDraftRow: vi.fn(),
    previewNewTourProcessingDraftRowWarnings: vi.fn(async () => []),
    previewTourProcessingDraftRowDayWarnings: vi.fn(async () => []),
    previewTourProcessingDraftRowWarnings: vi.fn(async () => []),
    updateTourProcessingDraftRow: vi.fn(),
    updateTourProcessingDraftRowDay: vi.fn(),
    updateTourObservationReviewed: vi.fn(),
}));

vi.mock('../../../hooks/useTourProcessingCatalogMatch.js', () => ({
    useTourProcessingCatalogMatch: () => ({
        driver: { options: [{ label: 'Jorge Francisco Mena' }], isMissing: (value) => value === 'Juan Chacsn', canAdd: true },
        vehicle: { options: [{ label: '301-627' }], isMissing: (value) => value === '999-999', canAdd: true },
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

function renderModal(mode, row = ROW, { queryClient = new QueryClient(), onClose = vi.fn(), onSaved = vi.fn() } = {}) {
    const rendered = render(
        <AppTheme defaultColorScheme="light">
            <QueryClientProvider client={queryClient}>
                <LocalizationProvider dateAdapter={AdapterDayjs}>
                    <TourProcessingDraftRowModal
                        open
                        mode={mode}
                        row={row}
                        draftId="draft-1"
                        onClose={onClose}
                        onSaved={onSaved}
                    />
                </LocalizationProvider>
            </QueryClientProvider>
        </AppTheme>,
    );
    return { queryClient, onClose, onSaved, ...rendered };
}

function keepRowsRefetchPending(queryClient) {
    const key = queryKeys.transport.tourProcessingDraftRows('draft-1', { page: 0 });
    queryClient.setQueryData(key, { rows: [ROW], totalElements: 1 });
    const observer = new QueryObserver(queryClient, { queryKey: key, queryFn: () => new Promise(() => {}), staleTime: Infinity });
    const unsubscribe = observer.subscribe(() => {});
    return { key, unsubscribe };
}

beforeEach(() => {
    updateTourProcessingDraftRow.mockReset();
    updateTourProcessingDraftRow.mockResolvedValue(ROW);
    updateTourProcessingDraftRowDay.mockReset();
    previewTourProcessingDraftRowWarnings.mockReset();
    previewTourProcessingDraftRowWarnings.mockResolvedValue([]);
    previewTourProcessingDraftRowDayWarnings.mockReset();
    previewTourProcessingDraftRowDayWarnings.mockResolvedValue([]);
});

afterEach(cleanup);

describe('TourProcessingDraftRowModal y recargas pendientes', () => {
    it('cierra al terminar el PUT y sustituye la fila antes de que termine el GET', async () => {
        const queryClient = new QueryClient();
        const { key, unsubscribe } = keepRowsRefetchPending(queryClient);
        updateTourProcessingDraftRow.mockResolvedValueOnce({ ...ROW, observations: 'Guardado', departureTime: '07:00:00' });
        const { onClose } = renderModal('edit', ROW, { queryClient });

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
        expect(queryClient.getQueryData(key).rows[0]).toEqual(expect.objectContaining({ observations: 'Guardado', departureTime: '07:00' }));
        expect(queryClient.getQueryState(key).fetchStatus).toBe('fetching');
        unsubscribe();
    });

    it('informa el número de la gira y lo que se cambió al guardar', async () => {
        const { onSaved } = renderModal('edit', ROW);

        await userEvent.type(screen.getByRole('textbox', { name: 'Observaciones' }), 'Salida temprano');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
        expect(onSaved).toHaveBeenCalledWith({
            created: false,
            number: '04040',
            changes: ['Observaciones: se agregó Salida temprano'],
        });
    });

    it('mantiene el modal y la caché al fallar el PUT', async () => {
        const queryClient = new QueryClient();
        const key = queryKeys.transport.tourProcessingDraftRows('draft-1', { page: 0 });
        queryClient.setQueryData(key, { rows: [ROW], totalElements: 1 });
        updateTourProcessingDraftRow.mockRejectedValueOnce(new Error('Fallo del servidor'));
        const { onClose } = renderModal('edit', ROW, { queryClient });

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Fallo del servidor')).toBeInTheDocument();
        expect(onClose).not.toHaveBeenCalled();
        expect(queryClient.getQueryData(key).rows[0]).toEqual(ROW);
    });

    it('descarta cambios locales al cerrar y reabrir', async () => {
        const queryClient = new QueryClient();
        const first = renderModal('edit', ROW, { queryClient });
        const observations = screen.getByRole('textbox', { name: 'Observaciones' });
        await userEvent.type(observations, 'Sin guardar');
        first.unmount();

        renderModal('edit', ROW, { queryClient });

        expect(screen.getByRole('textbox', { name: 'Observaciones' })).toHaveValue('');
    });
});

describe('TourProcessingDraftRowModal con prestamo de vehiculo', () => {
    it('no deja guardar si el chofer del prestamo no esta en el sistema', async () => {
        renderModal('edit', { ...ROW, resourceType: 'BORROWED_VEHICLE', borrowedVehicleId: 'b1', vehicle: null });

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText(/Agrégalo como chofer de otra unidad/)).toBeInTheDocument();
        expect(updateTourProcessingDraftRow).not.toHaveBeenCalled();
    });

    it('guarda el prestamo cuando el chofer esta en el sistema', async () => {
        renderModal('edit', { ...ROW, driver: 'Jorge Francisco Mena', resourceType: 'BORROWED_VEHICLE', borrowedVehicleId: 'b1', vehicle: null });

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
    });
});

describe('TourProcessingDraftRowModal con chofer y vehiculo fuera del sistema', () => {
    it('guarda la fila con el chofer vacio y el vehiculo elegido del sistema', async () => {
        renderModal('edit');

        expect(screen.getByText(/Este chofer no se encuentra en el sistema/)).toBeInTheDocument();
        expect(screen.getByText(/Este vehículo no se encuentra en el sistema/)).toBeInTheDocument();

        const [clearDriver] = screen.getAllByTitle('Clear');
        await userEvent.click(clearDriver);
        expect(screen.getByRole('combobox', { name: 'Chofer' })).toHaveValue('');
        expect(screen.queryByText(/Este chofer no se encuentra en el sistema/)).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', { name: 'Vehículo' }));
        await userEvent.click(await screen.findByRole('option', { name: '301-627' }));
        expect(screen.queryByText(/Este vehículo no se encuentra en el sistema/)).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
        expect(updateTourProcessingDraftRow).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            expect.objectContaining({ driver: null, vehicle: '301-627', number: '04040' }),
        );
    });

    it('permite guardar sin resolver las advertencias', async () => {
        renderModal('edit');

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
        expect(updateTourProcessingDraftRow).toHaveBeenCalledWith(
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

describe('TourProcessingDraftRowModal con la seccion Opciones', () => {
    it('muestra Opciones al final con el estado, el boton de aprobacion y Eliminar', () => {
        renderModal('edit');

        expect(screen.getByText('Opciones')).toBeInTheDocument();
        expect(screen.getByText('Estado: Aprobada')).toBeInTheDocument();
        expect(screen.getAllByRole('button', { name: 'No aprobar' })).toHaveLength(2);
        expect(screen.getByRole('button', { name: 'Eliminar' })).toBeInTheDocument();
    });

    it('repite el boton de aprobacion en el pie del modal y ambos cambian juntos', async () => {
        renderModal('edit');

        const [optionsButton, footerButton] = screen.getAllByRole('button', { name: 'No aprobar' });
        expect(optionsButton.closest('form')).not.toBeNull();
        expect(footerButton.closest('form')).toBeNull();

        await userEvent.click(footerButton);

        expect(screen.getByText('Estado: No aprobada')).toBeInTheDocument();
        expect(screen.getAllByRole('button', { name: 'Aprobar' })).toHaveLength(2);
        expect(screen.queryByRole('button', { name: 'No aprobar' })).not.toBeInTheDocument();
    });

    it('no muestra Opciones en modo ver', () => {
        renderModal('view');

        expect(screen.queryByText('Opciones')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Eliminar' })).not.toBeInTheDocument();
    });

    it('cambia a No aprobada y envia el estado solo al guardar', async () => {
        renderModal('edit');

        await userEvent.click(screen.getAllByRole('button', { name: 'No aprobar' })[0]);

        expect(screen.getByText('Estado: No aprobada')).toBeInTheDocument();
        expect(screen.getAllByRole('button', { name: 'Aprobar' })).toHaveLength(2);
        expect(updateTourProcessingDraftRow).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
        expect(updateTourProcessingDraftRow).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            expect.objectContaining({ approvalStatus: 'NOT_APPROVED' }),
        );
    });

    it('parte de No aprobada cuando la gira ya lo esta y permite volver a aprobarla', async () => {
        renderModal('edit', { ...ROW, approvalStatus: 'NOT_APPROVED' });

        expect(screen.getByText('Estado: No aprobada')).toBeInTheDocument();

        await userEvent.click(screen.getAllByRole('button', { name: 'Aprobar' })[0]);
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
        expect(updateTourProcessingDraftRow).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            expect.objectContaining({ approvalStatus: 'APPROVED' }),
        );
    });
});

describe('TourProcessingDraftRowModal al cambiar la duracion', () => {
    it('mueve la fecha de regreso segun la duracion y solo la envia al guardar', async () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null });

        const durationField = screen.getByRole('textbox', { name: /Duración/ });
        await userEvent.clear(durationField);
        await userEvent.type(durationField, '3');

        expect(screen.getByRole('group', { name: /Fecha regreso/ })).toHaveTextContent('04/05/2026');
        expect(updateTourProcessingDraftRow).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
        expect(updateTourProcessingDraftRow).toHaveBeenCalledWith(
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

describe('TourProcessingDraftRowModal con fechas de regreso invalidas', () => {
    it('marca la fecha de regreso anterior a la salida sin esperar al guardado y no permite guardar', async () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null, returnDate: '2026-05-01' });

        expect(screen.getByText('La fecha de regreso es anterior a la fecha de salida')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Revisa los datos antes de continuar')).toBeInTheDocument();
        expect(updateTourProcessingDraftRow).not.toHaveBeenCalled();
    });

    it('marca la hora de regreso anterior a la salida cuando es el mismo dia', async () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null, returnTime: '06:00' });

        expect(screen.getByText('La hora de regreso es anterior a la hora de salida')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Revisa los datos antes de continuar')).toBeInTheDocument();
        expect(updateTourProcessingDraftRow).not.toHaveBeenCalled();
    });

    it('no marca la hora de regreso anterior cuando la gira termina otro dia', () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null, returnDate: '2026-05-03', returnTime: '06:00' });

        expect(screen.queryByText('La hora de regreso es anterior a la hora de salida')).not.toBeInTheDocument();
    });
});

describe('TourProcessingDraftRowModal con pasajeros distribuidos', () => {
    it('permite guardar aunque la suma de los viajes no coincida con el total de pasajeros', async () => {
        renderModal('edit', { ...ROW, driver: null, vehicle: null });

        await userEvent.click(screen.getByRole('button', { name: 'Dividir en varios viajes' }));
        await userEvent.click(screen.getByRole('button', { name: 'Dividir' }));
        expect(screen.getByText('Viaje 1')).toBeInTheDocument();
        expect(screen.getByText('Viaje 2')).toBeInTheDocument();

        const passengerFields = await screen.findAllByRole('textbox', { name: 'Pasajeros' });
        await userEvent.clear(passengerFields[0]);
        await userEvent.type(passengerFields[0], '6');
        expect(screen.getByText('Faltan 2 pasajeros por cubrir.')).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
        expect(updateTourProcessingDraftRow).toHaveBeenCalledWith('draft-1', 'row-1', expect.objectContaining({
            passengers: 15,
            assignments: [
                expect.objectContaining({ passengers: 6 }),
                expect.objectContaining({ passengers: 7 }),
            ],
        }));
    });

    it('no marca error cuando el mismo chofer y vehículo se repiten en los viajes', async () => {
        renderModal('edit', {
            ...ROW,
            driver: 'Ana',
            vehicle: '301-627',
            assignments: [
                { rowId: 'a-1', driver: 'Ana', vehicle: '301-627', resourceType: 'INTERNAL', passengers: 8 },
                { rowId: 'a-2', driver: 'Ana', vehicle: '301-627', resourceType: 'INTERNAL', passengers: 7 },
            ],
        });

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
        expect(updateTourProcessingDraftRow).toHaveBeenCalledWith('draft-1', 'row-1', expect.objectContaining({
            assignments: [
                expect.objectContaining({ driver: 'Ana', vehicle: '301-627', passengers: 8 }),
                expect.objectContaining({ driver: 'Ana', vehicle: '301-627', passengers: 7 }),
            ],
        }));
    });
});

describe('TourProcessingDraftRowModal cuando la gira rompe una regla', () => {
    it('muestra el motivo y guarda solo si se confirma', async () => {
        previewTourProcessingDraftRowWarnings.mockResolvedValue([
            { code: 'DRIVER_OVERLAP', title: 'Choferes con giras que se cruzan', message: 'Ana: giras 100 y 200 (04/05/2026)' },
        ]);
        renderModal('edit', ROW);

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Esta gira rompe algunas reglas')).toBeInTheDocument();
        expect(screen.getByText(/Ana: giras 100 y 200/)).toBeInTheDocument();
        expect(updateTourProcessingDraftRow).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', { name: 'Revisar' }));
        expect(updateTourProcessingDraftRow).not.toHaveBeenCalled();
        await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Esta gira rompe algunas reglas' })).not.toBeInTheDocument());

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
        await userEvent.click(await screen.findByRole('button', { name: 'Guardar de todos modos' }));

        await waitFor(() => expect(updateTourProcessingDraftRow).toHaveBeenCalledTimes(1));
    });

    it('avisa al guardar un día y permite confirmar', async () => {
        previewTourProcessingDraftRowDayWarnings.mockResolvedValue([
            { code: 'SURPLUS_CAP_EXCEEDED', title: 'Choferes que superan el tope de sobresueldo del mes', message: 'Ana: 40 h de sobresueldo, tope de 37.5 h' },
        ]);
        updateTourProcessingDraftRowDay.mockResolvedValue({ ...ROW_WITH_DAYS.days[0], manuallyEdited: true });
        renderModal('edit', ROW_WITH_DAYS);

        await userEvent.click(screen.getByRole('button', { name: 'Editar día' }));
        await userEvent.click(screen.getByRole('button', { name: 'Guardar día' }));

        expect(await screen.findByText(/40 h de sobresueldo/)).toBeInTheDocument();
        expect(updateTourProcessingDraftRowDay).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar de todos modos' }));

        await waitFor(() => expect(updateTourProcessingDraftRowDay).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            'day-1',
            expect.objectContaining({ confirmOvertimeBelowSurplusThreshold: true }),
        ));
    });
});

describe('TourProcessingDraftRowModal cuando el chofer no puede atender la nueva duracion', () => {
    it('muestra una advertencia con el motivo que devuelve el servidor', async () => {
        updateTourProcessingDraftRow.mockRejectedValueOnce({
            response: {
                data: {
                    message: 'No se puede aumentar la duración de la gira porque el chofer asignado no puede atenderla.',
                    errors: ['TOUR_PROCESSING_DRAFT_ROW_DRIVER_UNAVAILABLE'],
                },
            },
        });
        renderModal('edit', { ...ROW, driver: null, vehicle: null });

        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Chofer no disponible')).toBeInTheDocument();
        expect(screen.getByText(/el chofer asignado no puede atenderla/)).toBeInTheDocument();
    });
});

describe('TourProcessingDraftRowModal con dias persistidos', () => {
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
        updateTourProcessingDraftRowDay.mockResolvedValue({
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
        const queryClient = new QueryClient();
        const { key, unsubscribe } = keepRowsRefetchPending(queryClient);
        renderModal('edit', ROW_WITH_DAYS, { queryClient });

        await userEvent.click(screen.getByRole('button', { name: 'Editar día' }));

        const surplusField = screen.getByRole('textbox', { name: 'Horas sobresueldo' });
        await userEvent.clear(surplusField);
        await userEvent.type(surplusField, '5');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar día' }));

        await waitFor(() => expect(updateTourProcessingDraftRowDay).toHaveBeenCalledTimes(1));
        expect(updateTourProcessingDraftRowDay).toHaveBeenCalledWith(
            'draft-1',
            'row-1',
            'day-1',
            expect.objectContaining({ startTime: '07:00', endTime: '19:00', ordinaryHours: 9, surplusHours: 5, overtimeHours: 0 }),
        );
        expect(await screen.findByText('Editado manualmente')).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: 'Horas sobresueldo' })).toHaveValue('5');
        expect(queryClient.getQueryState(key).fetchStatus).toBe('fetching');
        unsubscribe();
    });

    it('bloquea del lado del cliente mas de 9 horas ordinarias en un dia', async () => {
        renderModal('edit', ROW_WITH_DAYS);

        await userEvent.click(screen.getByRole('button', { name: 'Editar día' }));
        const ordinaryField = screen.getByRole('textbox', { name: 'Horas ordinarias' });
        await userEvent.clear(ordinaryField);
        await userEvent.type(ordinaryField, '10');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar día' }));

        expect(await screen.findByText('No puede superar las 9 horas')).toBeInTheDocument();
        expect(updateTourProcessingDraftRowDay).not.toHaveBeenCalled();
    });

    it('pide confirmacion para horas extra por debajo del 95% de sobresueldo y reintenta al confirmar', async () => {
        updateTourProcessingDraftRowDay.mockRejectedValueOnce({
            response: { data: { errors: ['TOUR_PROCESSING_DRAFT_ROW_DAY_OVERTIME_NEEDS_CONFIRMATION'] } },
        });
        updateTourProcessingDraftRowDay.mockResolvedValueOnce({
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

        await waitFor(() => expect(updateTourProcessingDraftRowDay).toHaveBeenCalledTimes(2));
        expect(updateTourProcessingDraftRowDay).toHaveBeenLastCalledWith(
            'draft-1',
            'row-1',
            'day-1',
            expect.objectContaining({ overtimeHours: 2, confirmOvertimeBelowSurplusThreshold: true }),
        );
        expect(await screen.findByText('Editado manualmente')).toBeInTheDocument();
    });
});
