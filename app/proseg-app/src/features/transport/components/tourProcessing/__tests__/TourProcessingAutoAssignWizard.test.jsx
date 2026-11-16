import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme.jsx';
import TourProcessingAutoAssignWizard from '../TourProcessingAutoAssignWizard.jsx';
import {
    discardAutoAssignWizard,
    finishAutoAssignWizard,
    recalculateAutoAssignWizard,
    saveAutoAssignWizard,
    setAutoAssignWizardStep,
} from '../../../services/tourProcessing/tourProcessingService';
import { useTourProcessingDraftRows } from '../../../hooks/useTourProcessingDraftsData.js';

vi.mock('../../../services/tourProcessing/tourProcessingService', () => ({
    discardAutoAssignWizard: vi.fn(),
    finishAutoAssignWizard: vi.fn(),
    recalculateAutoAssignWizard: vi.fn(),
    saveAutoAssignWizard: vi.fn(),
    setAutoAssignWizardStep: vi.fn(),
}));

vi.mock('../../../hooks/useTourProcessingDraftsData.js', () => ({
    invalidateTourProcessingDraft: vi.fn(() => Promise.resolve()),
    useTourProcessingDraftRowProblems: vi.fn(() => ({ problemsByRowId: {} })),
    useTourProcessingDraftRows: vi.fn(),
}));

vi.mock('../useDraftRowColumns.jsx', () => ({
    DRAFT_ROWS_FILTERABLE_COLUMNS: new Set(),
    useDraftRowColumns: vi.fn(() => []),
}));

vi.mock('../DraftRowsTable.jsx', () => ({
    default: ({ rows, onOpenRow }) => (
        <div data-testid="rows-table">
            {rows.map((row) => <button key={row.id} type="button" onClick={() => onOpenRow('edit', row)}>{row.number}</button>)}
        </div>
    ),
}));

const STEPS = [
    {
        code: 'MANDATORY', label: 'Prioridad 1 con menos de 5 pasajeros', filterRule: 'WIZARD_STEP:MANDATORY',
        tours: 3, complete: 2, withoutDriver: 1, withoutVehicle: 0,
        drivers: [{ driver: 'Ana Mora', tours: 2, tourNumbers: ['1001', '1002'] }, { driver: 'Beto Soto', tours: 1, tourNumbers: ['1003'] }],
    },
    {
        code: 'PERIPHERAL', label: 'Giras a la periférica', filterRule: 'WIZARD_STEP:PERIPHERAL',
        tours: 2, complete: 2, withoutDriver: 0, withoutVehicle: 0,
        drivers: [{ driver: 'Carla Vega', tours: 2, tourNumbers: ['2001', '2002'] }],
    },
];

function wizardState(overrides = {}) {
    return { active: true, currentStep: 'MANDATORY', dirty: false, steps: STEPS, lastResult: null, ...overrides };
}

function setup(wizard = wizardState()) {
    const props = {
        draftId: 'draft-1',
        wizard,
        subtitle: 'Mayo 2026',
        canEdit: true,
        onClose: vi.fn(),
        onFinished: vi.fn(),
        onOpenRow: vi.fn(),
        onObservationReviewed: vi.fn(),
        onMessage: vi.fn(),
    };
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
        <QueryClientProvider client={queryClient}>
            <AppTheme defaultColorScheme="light">
                <TourProcessingAutoAssignWizard {...props} />
            </AppTheme>
        </QueryClientProvider>,
    );
    return props;
}

beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useTourProcessingDraftRows.mockImplementation((_draftId, params) => ({
        rows: params.rule === 'WIZARD_STEP:MANDATORY'
            ? [{ id: 'r1', number: '1001' }, { id: 'r2', number: '1002' }]
            : [{ id: 'r3', number: '2001' }],
        loading: false,
        fetching: false,
        error: null,
        totalElements: 2,
    }));
    setAutoAssignWizardStep.mockImplementation((_draftId, code) => Promise.resolve(wizardState({ currentStep: code })));
});

afterEach(cleanup);

describe('asistente de autoasignación paso a paso', () => {
    it('muestra el paso actual, permite elegir otro y filtra la tabla por el paso', () => {
        setup();

        expect(screen.getByText('Paso 1 de 2 - 3 giras')).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'Prioridad 1 con menos de 5 pasajeros' })).toBeInTheDocument();
        const selector = screen.getByRole('combobox', { name: 'Ir a otro paso' });
        expect(within(selector).getAllByRole('option')).toHaveLength(3);
        expect(within(selector).getByRole('option', { name: '2. Giras a la periférica - 2 giras' })).toBeInTheDocument();
        expect(screen.queryByText('Ana Mora · 2')).not.toBeInTheDocument();
        expect(screen.getByText('1 sin chofer')).toBeInTheDocument();
        expect(screen.getByText('2 completas')).toBeInTheDocument();
        expect(within(screen.getByTestId('rows-table')).getAllByRole('button').map((button) => button.textContent)).toEqual(['1001', '1002']);
        expect(useTourProcessingDraftRows).toHaveBeenLastCalledWith('draft-1', expect.objectContaining({ rule: 'WIZARD_STEP:MANDATORY' }), { enabled: true });
    });

    it('abre la gira para editarla con el mismo modal de la tabla de afuera', async () => {
        const user = userEvent.setup();
        const props = setup();

        await user.click(screen.getByRole('button', { name: '1001' }));

        expect(props.onOpenRow).toHaveBeenCalledWith('edit', { id: 'r1', number: '1001' });
    });

    it('al aceptar guarda el paso siguiente y muestra sus giras sin recalcular si no hubo cambios', async () => {
        const user = userEvent.setup();
        setup();

        await user.click(screen.getByRole('button', { name: 'Aceptar y continuar' }));

        await waitFor(() => expect(screen.getByRole('heading', { name: 'Giras a la periférica' })).toBeInTheDocument());
        expect(screen.getByText('Paso 2 de 2 - 2 giras')).toBeInTheDocument();
        expect(setAutoAssignWizardStep).toHaveBeenCalledWith('draft-1', 'PERIPHERAL');
        expect(recalculateAutoAssignWizard).not.toHaveBeenCalled();
        expect(screen.getByRole('button', { name: 'Terminar autoasignación' })).toBeInTheDocument();
    });

    it('recalcula antes de pasar al siguiente paso cuando hay cambios manuales', async () => {
        const user = userEvent.setup();
        recalculateAutoAssignWizard.mockResolvedValue(wizardState({ dirty: false }));
        setup(wizardState({ dirty: true }));

        expect(screen.getByText(/Hiciste cambios manuales/)).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Aceptar y continuar' }));

        await waitFor(() => expect(setAutoAssignWizardStep).toHaveBeenCalledWith('draft-1', 'PERIPHERAL'));
        expect(recalculateAutoAssignWizard).toHaveBeenCalledWith('draft-1');
    });

    it('permite volver a un paso anterior', async () => {
        const user = userEvent.setup();
        setup(wizardState({ currentStep: 'PERIPHERAL' }));

        expect(screen.getByRole('button', { name: 'Anterior' })).toBeEnabled();
        await user.click(screen.getByRole('button', { name: 'Anterior' }));

        await waitFor(() => expect(screen.getByRole('heading', { name: 'Prioridad 1 con menos de 5 pasajeros' })).toBeInTheDocument());
        expect(setAutoAssignWizardStep).toHaveBeenCalledWith('draft-1', 'MANDATORY');
    });

    it('permite ver el paso siguiente sin aceptarlo', async () => {
        const user = userEvent.setup();
        setup();

        await user.click(screen.getByRole('button', { name: 'Siguiente' }));

        await waitFor(() => expect(screen.getByRole('heading', { name: 'Giras a la periférica' })).toBeInTheDocument());
        expect(setAutoAssignWizardStep).toHaveBeenCalledWith('draft-1', 'PERIPHERAL');
        expect(recalculateAutoAssignWizard).not.toHaveBeenCalled();
        expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
    });

    it('permite saltar a otro paso desde el selector', async () => {
        const user = userEvent.setup();
        setup();

        await user.selectOptions(screen.getByRole('combobox', { name: 'Ir a otro paso' }), 'PERIPHERAL');

        await waitFor(() => expect(screen.getByRole('heading', { name: 'Giras a la periférica' })).toBeInTheDocument());
        expect(setAutoAssignWizardStep).toHaveBeenCalledWith('draft-1', 'PERIPHERAL');
        expect(within(screen.getByTestId('rows-table')).getAllByRole('button').map((button) => button.textContent)).toEqual(['2001']);
    });

    it('permite poner el modal en pantalla completa y volver al tamaño normal', async () => {
        const user = userEvent.setup();
        setup();
        const dialog = screen.getByRole('dialog');
        expect(dialog).not.toHaveClass('MuiDialog-paperFullScreen');

        await user.click(screen.getByRole('button', { name: 'Pantalla completa' }));
        expect(dialog).toHaveClass('MuiDialog-paperFullScreen');

        await user.click(screen.getByRole('button', { name: 'Salir de pantalla completa' }));
        expect(dialog).not.toHaveClass('MuiDialog-paperFullScreen');
    });

    it('no deja retroceder desde el primer paso', () => {
        setup();

        expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    });

    it('al cancelar ofrece guardar el progreso o descartar todos los cambios', async () => {
        const user = userEvent.setup();
        saveAutoAssignWizard.mockResolvedValue(wizardState());
        const props = setup();

        await user.click(screen.getByRole('button', { name: 'Cancelar' }));
        expect(await screen.findByText('Salir de la autoasignación')).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Guardar y salir' }));

        await waitFor(() => expect(saveAutoAssignWizard).toHaveBeenCalledWith('draft-1', 'MANDATORY'));
        await waitFor(() => expect(props.onClose).toHaveBeenCalled());
        expect(props.onMessage).toHaveBeenCalledWith('success', expect.stringContaining('Se guardó el progreso'));
    });

    it('descarta los cambios y avisa que el borrador volvió al estado inicial', async () => {
        const user = userEvent.setup();
        discardAutoAssignWizard.mockResolvedValue(undefined);
        const props = setup();

        await user.click(screen.getByRole('button', { name: 'Cancelar' }));
        await user.click(await screen.findByRole('button', { name: 'Descartar cambios' }));

        await waitFor(() => expect(discardAutoAssignWizard).toHaveBeenCalledWith('draft-1'));
        await waitFor(() => expect(props.onClose).toHaveBeenCalled());
        expect(props.onMessage).toHaveBeenCalledWith('success', expect.stringContaining('Se descartaron los cambios'));
    });

    it('en el último paso termina la autoasignación y entrega el resultado', async () => {
        const user = userEvent.setup();
        finishAutoAssignWizard.mockResolvedValue(undefined);
        const lastResult = { drivers: { assignedTours: 3 }, vehicles: { assignedTours: 3 } };
        const props = setup(wizardState({ currentStep: 'PERIPHERAL', lastResult }));

        await user.click(screen.getByRole('button', { name: 'Terminar autoasignación' }));
        await user.click(await screen.findByRole('button', { name: 'Terminar' }));

        await waitFor(() => expect(finishAutoAssignWizard).toHaveBeenCalledWith('draft-1'));
        await waitFor(() => expect(props.onFinished).toHaveBeenCalledWith(lastResult));
        expect(props.onClose).toHaveBeenCalled();
    });

    it('recalcula a pedido del usuario', async () => {
        const user = userEvent.setup();
        recalculateAutoAssignWizard.mockResolvedValue(wizardState());
        setup();

        await user.click(screen.getByRole('button', { name: 'Recalcular' }));

        await waitFor(() => expect(recalculateAutoAssignWizard).toHaveBeenCalledWith('draft-1'));
    });

    it('avisa cuando una acción falla', async () => {
        const user = userEvent.setup();
        recalculateAutoAssignWizard.mockRejectedValue(new Error('falló'));
        const props = setup();

        await user.click(screen.getByRole('button', { name: 'Recalcular' }));

        await waitFor(() => expect(props.onMessage).toHaveBeenCalledWith('error', expect.any(String)));
    });
});
