import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingDraftPage from '../TourProcessingDraftPage.jsx';
import { fetchTourProcessingDraftRows, finishTourProcessingDraft, reopenTourProcessingDraft, updateTourProcessingDraftRowApprovalStatus } from '../../../services/tourProcessing/tourProcessingService.js';
import { queryKeys } from '../../../../../common/query';

vi.mock('../../../../../common/hooks/index.js', () => ({
    usePermissions: () => ({ hasAnyPermission: () => true }),
}));

vi.mock('../../../../../common/components/TablaBase.jsx', () => ({
    default: ({ data, renderRowActions }) => <div>{data.map((item) => (
        <section key={item.id} data-testid={`row-${item.id}`}>
            <span>{item.approvalStatus}</span>
            {renderRowActions({ row: { original: item } })}
        </section>
    ))}</div>,
}));

vi.mock('../../../../../common/components/RowActionsMenu.jsx', () => ({
    default: ({ actions }) => <div>{actions.map((action) => (
        <button key={action.key} type="button" disabled={action.disabled} onClick={action.onClick}>{action.label}</button>
    ))}</div>,
}));

vi.mock('../../../components/tourProcessing/TourProcessingDraftRowModal.jsx', () => ({ default: () => null }));
vi.mock('../../../components/tourProcessing/TourProcessingDriverAssignmentReport.jsx', () => ({ default: () => null }));

vi.mock('../../../hooks/useTourProcessingCatalogMatch.js', () => ({
    useTourProcessingCatalogMatch: () => ({
        driver: { isMissing: () => false },
        vehicle: { isMissing: () => false, catalog: [] },
    }),
}));

vi.mock('../../../hooks/useTransportOptions.js', () => ({
    useBorrowedVehicleOptions: () => ({ options: [] }),
}));

vi.mock('../../../services/tourProcessing/tourProcessingService.js', () => ({
    fetchTourProcessingDraft: vi.fn(),
    fetchTourProcessingDraftRows: vi.fn(),
    finishTourProcessingDraft: vi.fn(),
    reopenTourProcessingDraft: vi.fn(),
    updateTourProcessingDraftRowApprovalStatus: vi.fn(),
}));

const rows = [
    { id: 'row-1', number: '100', approvalStatus: 'APPROVED' },
    { id: 'row-2', number: '200', approvalStatus: 'APPROVED' },
];

beforeEach(async () => {
    localStorage.clear();
    const { fetchTourProcessingDraft } = await import('../../../services/tourProcessing/tourProcessingService.js');
    fetchTourProcessingDraft.mockReset();
    fetchTourProcessingDraft.mockResolvedValue({ id: 'draft-1', status: 'DRAFT', year: 2026, month: 5 });
    fetchTourProcessingDraftRows.mockReset();
    fetchTourProcessingDraftRows.mockResolvedValueOnce({ content: rows, totalElements: 2 });
    fetchTourProcessingDraftRows.mockImplementation(() => new Promise(() => {}));
    updateTourProcessingDraftRowApprovalStatus.mockReset();
    updateTourProcessingDraftRowApprovalStatus.mockImplementation((draftId, rowId) => Promise.resolve({
        ...rows.find((row) => row.id === rowId),
        approvalStatus: 'NOT_APPROVED',
    }));
    finishTourProcessingDraft.mockReset();
    finishTourProcessingDraft.mockResolvedValue({ id: 'draft-1', status: 'CLOSED', year: 2026, month: 5 });
    reopenTourProcessingDraft.mockReset();
    reopenTourProcessingDraft.mockResolvedValue({ id: 'draft-1', status: 'DRAFT', year: 2026, month: 5 });
});

describe('estado de depuración', () => {
    it('termina el borrador y abre el historial', async () => {
        render(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={new QueryClient()}>
                    <MemoryRouter initialEntries={['/transporte/depuracion/borradores/draft-1']}>
                        <Routes>
                            <Route path="/transporte/depuracion/borradores/:draftId" element={<TourProcessingDraftPage />} />
                            <Route path="/transporte/depuracion" element={<div>Historial abierto</div>} />
                        </Routes>
                    </MemoryRouter>
                </QueryClientProvider>
            </AppTheme>,
        );

        await screen.findByRole('button', { name: 'Opciones' });
        await userEvent.click(screen.getByRole('button', { name: 'Opciones' }));
        expect(within(screen.getByRole('menu')).getAllByRole('menuitem').at(-1)).toHaveTextContent('Terminar depuración');
        await userEvent.click(screen.getByRole('menuitem', { name: 'Terminar depuración' }));
        await userEvent.click(screen.getByRole('button', { name: 'Terminar depuración' }));

        await waitFor(() => expect(finishTourProcessingDraft).toHaveBeenCalledWith('draft-1'));
        expect(await screen.findByText('Historial abierto')).toBeInTheDocument();
    });

    it('devuelve una depuración terminada a borrador', async () => {
        const { fetchTourProcessingDraft } = await import('../../../services/tourProcessing/tourProcessingService.js');
        fetchTourProcessingDraft
            .mockResolvedValueOnce({ id: 'draft-1', status: 'CLOSED', year: 2026, month: 5 })
            .mockResolvedValue({ id: 'draft-1', status: 'DRAFT', year: 2026, month: 5 });
        const queryClient = new QueryClient();
        render(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/transporte/depuracion/borradores/draft-1']}>
                        <Routes>
                            <Route path="/transporte/depuracion/borradores/:draftId" element={<TourProcessingDraftPage />} />
                        </Routes>
                    </MemoryRouter>
                </QueryClientProvider>
            </AppTheme>,
        );

        await screen.findByRole('button', { name: 'Opciones' });
        const closedRow = await screen.findByTestId('row-row-1');
        expect(within(closedRow).getByRole('button', { name: 'Editar', hidden: true })).toBeDisabled();
        await userEvent.click(screen.getByRole('button', { name: 'Opciones' }));
        await userEvent.click(screen.getByRole('menuitem', { name: 'Devolver a borrador' }));
        await userEvent.click(screen.getByRole('button', { name: 'Devolver a borrador' }));

        await waitFor(() => expect(reopenTourProcessingDraft).toHaveBeenCalledWith('draft-1'));
        await waitFor(() => expect(queryClient.getQueryData(queryKeys.transport.tourProcessingDraftDetail('draft-1')).status).toBe('DRAFT'));
        await waitFor(() => expect(within(closedRow).getByRole('button', { name: 'Editar', hidden: true })).toBeEnabled());
    });
});

afterEach(() => {
    cleanup();
    localStorage.clear();
});

describe('aprobación de filas', () => {
    it('permite aprobar otra fila mientras el GET de recarga sigue pendiente', async () => {
        const queryClient = new QueryClient();
        render(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/transporte/depuracion/borradores/draft-1']}>
                        <Routes>
                            <Route path="/transporte/depuracion/borradores/:draftId" element={<TourProcessingDraftPage />} />
                        </Routes>
                    </MemoryRouter>
                </QueryClientProvider>
            </AppTheme>,
        );

        const first = await screen.findByTestId('row-row-1');
        await waitFor(() => expect(within(first).getByRole('button', { name: 'No aprobar', hidden: true })).toBeEnabled());
        await userEvent.click(within(first).getByRole('button', { name: 'No aprobar', hidden: true }));

        await waitFor(() => expect(within(first).getByText('NOT_APPROVED')).toBeInTheDocument());
        expect(fetchTourProcessingDraftRows).toHaveBeenCalledTimes(2);
        expect(within(screen.getByTestId('row-row-2')).getByRole('button', { name: 'No aprobar', hidden: true })).toBeEnabled();

        await userEvent.click(within(screen.getByTestId('row-row-2')).getByRole('button', { name: 'No aprobar', hidden: true }));

        await waitFor(() => expect(updateTourProcessingDraftRowApprovalStatus).toHaveBeenCalledTimes(2));
        expect(within(screen.getByTestId('row-row-2')).getByText('NOT_APPROVED')).toBeInTheDocument();
    });
});
