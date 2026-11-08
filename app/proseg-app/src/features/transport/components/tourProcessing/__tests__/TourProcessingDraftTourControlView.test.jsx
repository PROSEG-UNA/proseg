import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme.jsx';
import TourProcessingDraftTourControlView from '../TourProcessingDraftTourControlView.jsx';
import { useTourProcessingDraftDriverSummaries, useTourProcessingDraftSchedule } from '../../../hooks/useTourProcessingDraftsData.js';

vi.mock('../../../hooks/useTourProcessingDraftsData.js', () => ({
    useTourProcessingDraftSchedule: vi.fn(),
    useTourProcessingDraftDriverSummaries: vi.fn(),
}));

const DRIVERS = [
    {
        id: 'ana', name: 'Ana Mora', surplusHours: 12, overtimeHours: 6,
        tours: [
            {
                id: 'tour-1', number: '3622', departureDate: '2026-05-01', returnDate: '2026-05-02',
                destination: 'Turrialba', vehicle: '301-627', responsible: 'Carlos Solano', passengers: 3, priority: 1,
                days: [
                    { date: '2026-05-01', ordinaryHours: 8, surplusHours: 2, overtimeHours: 0 },
                    { date: '2026-05-02', ordinaryHours: 0, surplusHours: 4, overtimeHours: 3 },
                ],
            },
            {
                id: 'tour-2', number: '3700', departureDate: '2026-05-02', returnDate: '2026-05-02',
                destination: 'Heredia', vehicle: '301-888', responsible: 'María Pérez', passengers: 2, priority: 2,
                days: [{ date: '2026-05-02', ordinaryHours: 0, surplusHours: 6, overtimeHours: 3 }],
            },
        ],
    },
    { id: 'luis', name: 'Luis Castro', surplusHours: 0, overtimeHours: 0, tours: [] },
    { id: 'rosa', name: 'Rosa Vega', surplusHours: 0, overtimeHours: 0, tours: [] },
    { id: 'mario', name: 'Mario Soto', surplusHours: 0, overtimeHours: 0, tours: [] },
];

const SUMMARIES = [
    { id: 'ana', organizationalUnit: 'GENERAL_SERVICES', status: 'ACTIVE' },
    { id: 'luis', organizationalUnit: 'GENERAL_SERVICES', status: 'ACTIVE' },
    { id: 'rosa', organizationalUnit: 'GENERAL_SERVICES', status: 'INACTIVE' },
    { id: 'mario', organizationalUnit: 'OTHER_UNIT', status: 'ACTIVE' },
];

function renderView(props = {}) {
    return render(
        <AppTheme>
            <TourProcessingDraftTourControlView draftId="draft-1" year={2026} month={5} {...props} />
        </AppTheme>,
    );
}

beforeEach(() => {
    useTourProcessingDraftDriverSummaries.mockReturnValue({ surplusCapPerDriver: 40, drivers: SUMMARIES, loading: false, fetching: false, error: null });
    useTourProcessingDraftSchedule.mockReturnValue({ drivers: DRIVERS, loading: false, fetching: false, error: null });
});

afterEach(cleanup);

describe('Control de giras', () => {
    it('filtra por unidad y estado, con Servicios Generales y Activo por defecto', async () => {
        renderView();
        expect(screen.getByText('Ana Mora')).toBeTruthy();
        expect(screen.getByText('Luis Castro')).toBeTruthy();
        expect(screen.queryByText('Rosa Vega')).toBeNull();
        expect(screen.queryByText('Mario Soto')).toBeNull();
    });

    it('muestra todos los choferes al elegir Todas las unidades y Todos los estados', async () => {
        const user = userEvent.setup();
        renderView();
        await user.click(screen.getByRole('combobox', { name: 'Unidad' }));
        await user.click(screen.getByRole('option', { name: 'Todas' }));
        await user.click(screen.getByRole('combobox', { name: 'Estado' }));
        await user.click(screen.getByRole('option', { name: 'Todos' }));
        expect(screen.getByText('Rosa Vega')).toBeTruthy();
        expect(screen.getByText('Mario Soto')).toBeTruthy();
    });

    it('muestra las horas de sobresueldo del mes', () => {
        renderView();
        expect(screen.getByText('Horas sobresueldo del mes')).toBeTruthy();
        expect(screen.getByText(/40.*h por chofer/)).toBeTruthy();
    });

    it.each([[true, 'edit'], [false, 'view']])('abre la gira por su id en modo %s/%s al tocar el detalle', async (canEdit, mode) => {
        const onOpenRowById = vi.fn();
        renderView({ canEdit, onOpenRowById });
        await userEvent.click(screen.getByText('Ana Mora'));
        await userEvent.click(screen.getByText('3622'));

        expect(onOpenRowById).toHaveBeenCalledExactlyOnceWith(mode, 'tour-1');
        expect(screen.getByRole('list', { name: 'Detalle de giras de Ana Mora' })).toBeInTheDocument();
    });

    it('abre la gira desde el teclado', async () => {
        const onOpenRowById = vi.fn();
        renderView({ canEdit: true, onOpenRowById });
        await userEvent.click(screen.getByText('Ana Mora'));
        const detail = screen.getByRole('list', { name: 'Detalle de giras de Ana Mora' });
        within(detail).getByRole('button', { name: 'Abrir gira 3622' }).focus();
        await userEvent.keyboard('{Enter}');

        expect(onOpenRowById).toHaveBeenCalledExactlyOnceWith('edit', 'tour-1');
    });

    it('inicia el primer día, muestra todos los choferes y conserva los totales mensuales', () => {
        renderView();

        expect(useTourProcessingDraftSchedule).toHaveBeenLastCalledWith('draft-1', '2026-05-01');
        expect(screen.getByRole('button', { name: 'Día anterior' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Día siguiente' })).toBeEnabled();
        expect(screen.getAllByText('01/05/2026')).toHaveLength(2);
        const ana = screen.getByRole('row', { name: /Ana Mora/ });
        expect(within(ana).getByText('12')).toBeInTheDocument();
        expect(within(ana).getByText('6')).toBeInTheDocument();
        expect(within(screen.getByRole('row', { name: /Luis Castro/ })).getByRole('button', { name: /Expandir/ })).toBeDisabled();
    });

    it('expande solamente las giras del día con horas diarias y mantiene las fechas completas de la gira', async () => {
        renderView();
        await userEvent.click(screen.getByText('Ana Mora'));
        const detail = screen.getByRole('list', { name: 'Detalle de giras de Ana Mora' });
        expect(within(detail).getByText('3622')).toBeInTheDocument();
        expect(within(detail).queryByText('3700')).not.toBeInTheDocument();
        expect(within(detail).getByText('02/05/2026')).toBeInTheDocument();
        expect(within(detail).getByText('Turrialba')).toBeInTheDocument();
        expect(within(detail).getByText('301-627')).toBeInTheDocument();
        expect(within(detail).getByText('Carlos Solano')).toBeInTheDocument();
        const tour = within(detail).getByRole('button', { name: 'Abrir gira 3622' });
        expect(within(tour).getAllByRole('definition').map((field) => field.textContent))
            .toEqual(['3622', '8 h', '2 h', '0 h', '01/05/2026', '02/05/2026', 'Turrialba', '301-627', 'Carlos Solano', '3', '1']);

        await userEvent.click(screen.getByRole('button', { name: 'Día siguiente' }));
        expect(useTourProcessingDraftSchedule).toHaveBeenLastCalledWith('draft-1', '2026-05-02');
        await waitFor(() => expect(screen.queryByRole('list', { name: 'Detalle de giras de Ana Mora' })).not.toBeInTheDocument());
        await userEvent.click(screen.getByText('Ana Mora'));
        const nextDetail = screen.getByRole('list', { name: 'Detalle de giras de Ana Mora' });
        expect(within(nextDetail).getAllByRole('listitem')).toHaveLength(2);
        const nextTour = within(nextDetail).getByRole('button', { name: 'Abrir gira 3622' });
        expect(within(nextTour).getAllByRole('definition').slice(1, 4).map((field) => field.textContent)).toEqual(['0 h', '4 h', '3 h']);
        expect(within(nextDetail).getByText('3700')).toBeInTheDocument();
        expect(within(screen.getByText('Ana Mora').closest('tr')).getByText('12')).toBeInTheDocument();
    });

    it.each([[2026, 2, 28], [2028, 2, 29], [2026, 4, 30], [2026, 5, 31]])('bloquea la navegación al terminar %s-%s', async (year, month, lastDay) => {
        useTourProcessingDraftSchedule.mockReturnValue({ drivers: [], loading: false, fetching: false, error: null });
        renderView({ year, month });
        const next = screen.getByRole('button', { name: 'Día siguiente' });
        for (let day = 1; day < lastDay; day += 1) await userEvent.click(next);
        expect(next).toBeDisabled();
        expect(screen.getByRole('spinbutton', { name: 'Dia' })).toHaveAttribute('aria-valuenow', String(lastDay));
        await userEvent.click(screen.getByRole('button', { name: 'Día anterior' }));
        expect(next).toBeEnabled();
    });

    it('permite elegir un día directamente desde el calendario', async () => {
        renderView();
        await userEvent.click(screen.getByRole('button', { name: /Elige fecha/ }));
        await userEvent.click(screen.getByRole('gridcell', { name: '15', exact: true }));
        await userEvent.click(screen.getByRole('button', { name: 'OK', exact: true }));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(screen.getAllByText('15/05/2026')).toHaveLength(2);
        expect(within(screen.getByRole('row', { name: /Ana Mora/ })).getByRole('button', { name: /Expandir/ })).toBeDisabled();
    });

    it('muestra los errores de carga', () => {
        useTourProcessingDraftSchedule.mockReturnValue({ drivers: [], loading: false, fetching: false, error: 'No se pudo cargar el control de giras' });
        renderView();
        expect(screen.getByText('No se pudo cargar el control de giras')).toBeInTheDocument();
    });
});
