import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingDraftPage from '../TourProcessingDraftPage.jsx';
import { checkTourProcessingDraftFinalCommission, exportTourProcessingDraftFinalCommission, fetchTourProcessingDraftRow, fetchTourProcessingDraftRowProblems, fetchTourProcessingDraftRows, finishTourProcessingDraft, previewTourProcessingDraftApprovalWarnings, rejectTourProcessingDraftIncompleteTours, reopenTourProcessingDraft, updateTourProcessingDraftRowApprovalStatus } from '../../../services/tourProcessing/tourProcessingService.js';
import { queryKeys } from '../../../../../common/query';

const granted = vi.hoisted(() => ({ permissions: null }));

vi.mock('../../../../../common/hooks/index.js', () => ({
    usePermissions: () => ({
        hasAnyPermission: (permissions) => granted.permissions === null
            || permissions.some((permission) => granted.permissions.includes(permission)),
    }),
}));

vi.mock('../../../../../common/components/TablaBase.jsx', () => ({
    default: ({ data, columns, renderRowActions }) => <div>{data.map((item) => (
        <section key={item.id} data-testid={`row-${item.id}`}>
            <span>{item.approvalStatus}</span>
            {columns.filter((column) => column.accessorKey === 'number' || column.accessorKey === 'observations').map((column) => (
                <div key={column.id ?? column.accessorKey} data-testid={`cell-${column.id ?? column.accessorKey}-${item.id}`}>
                    {column.Cell({ cell: { getValue: () => item[column.accessorKey] }, row: { original: item } })}
                </div>
            ))}
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

vi.mock('../../../services/tourProcessing/tourProcessingService.js', () => ({
    fetchTourProcessingDraft: vi.fn(),
    fetchTourProcessingDraftRows: vi.fn(),
    fetchTourProcessingDraftRow: vi.fn(),
    fetchTourProcessingDraftRowProblems: vi.fn(async () => ({})),
    checkTourProcessingDraftFinalCommission: vi.fn(),
    exportTourProcessingDraftFinalCommission: vi.fn(),
    rejectTourProcessingDraftIncompleteTours: vi.fn(),
    finishTourProcessingDraft: vi.fn(),
    reopenTourProcessingDraft: vi.fn(),
    previewTourProcessingDraftApprovalWarnings: vi.fn(async () => []),
    updateTourProcessingDraftRowApprovalStatus: vi.fn(),
    updateTourObservationReviewed: vi.fn(),
}));

const rows = [
    { id: 'row-1', number: '100', approvalStatus: 'APPROVED' },
    { id: 'row-2', number: '200', approvalStatus: 'APPROVED' },
];

const renderDraftPage = () => render(
    <AppTheme defaultColorScheme="light">
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter initialEntries={['/transporte/depuracion/borradores/draft-1']}>
                <Routes>
                    <Route path="/transporte/depuracion/borradores/:draftId" element={<TourProcessingDraftPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>
    </AppTheme>,
);

const openOptions = async () => {
    await userEvent.click(await screen.findByRole('button', { name: 'Opciones' }));
    return within(screen.getByRole('menu')).getAllByRole('menuitem').map((item) => item.textContent);
};

beforeEach(async () => {
    granted.permissions = null;
    localStorage.clear();
    const { fetchTourProcessingDraft } = await import('../../../services/tourProcessing/tourProcessingService.js');
    fetchTourProcessingDraft.mockReset();
    fetchTourProcessingDraft.mockResolvedValue({ id: 'draft-1', status: 'DRAFT', year: 2026, month: 5 });
    fetchTourProcessingDraftRows.mockReset();
    fetchTourProcessingDraftRows.mockResolvedValueOnce({ content: rows, totalElements: 2 });
    fetchTourProcessingDraftRows.mockImplementation(() => new Promise(() => {}));
    previewTourProcessingDraftApprovalWarnings.mockReset();
    previewTourProcessingDraftApprovalWarnings.mockResolvedValue([]);
    updateTourProcessingDraftRowApprovalStatus.mockReset();
    updateTourProcessingDraftRowApprovalStatus.mockImplementation((draftId, rowId) => Promise.resolve({
        ...rows.find((row) => row.id === rowId),
        approvalStatus: 'NOT_APPROVED',
    }));
    checkTourProcessingDraftFinalCommission.mockReset();
    exportTourProcessingDraftFinalCommission.mockReset();
    rejectTourProcessingDraftIncompleteTours.mockReset();
    fetchTourProcessingDraftRow.mockReset();
    fetchTourProcessingDraftRow.mockResolvedValue({ id: 'row-1', number: '100', approvalStatus: 'APPROVED' });
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

describe('permisos del borrador', () => {
    it('solo deja ver el borrador con LEER_GIRAS', async () => {
        granted.permissions = ['LEER_GIRAS'];
        renderDraftPage();

        const row = await screen.findByTestId('row-row-1');
        expect(within(row).getByRole('button', { name: 'Editar' })).toBeDisabled();
        expect(within(row).getByRole('button', { name: 'Eliminar' })).toBeDisabled();
        expect(screen.queryByRole('button', { name: 'Opciones' })).toBeNull();
    });

    it('con CREAR_GIRAS permite crear pero no editar', async () => {
        granted.permissions = ['CREAR_GIRAS'];
        renderDraftPage();

        const row = await screen.findByTestId('row-row-1');
        expect(within(row).getByRole('button', { name: 'Editar' })).toBeDisabled();
        expect(await openOptions()).toEqual(['Crear gira']);
    });

    it('con EDITAR_GIRAS permite editar sin eliminar', async () => {
        granted.permissions = ['EDITAR_GIRAS'];
        renderDraftPage();

        const row = await screen.findByTestId('row-row-1');
        expect(within(row).getByRole('button', { name: 'Editar' })).toBeEnabled();
        expect(within(row).getByRole('button', { name: 'Eliminar' })).toBeDisabled();
        const options = await openOptions();
        expect(options).toContain('Terminar depuración');
        expect(options).not.toContain('Crear gira');
    });

    it('con ELIMINAR_GIRAS permite eliminar sin editar', async () => {
        granted.permissions = ['ELIMINAR_GIRAS'];
        renderDraftPage();

        const row = await screen.findByTestId('row-row-1');
        expect(within(row).getByRole('button', { name: 'Eliminar' })).toBeEnabled();
        expect(within(row).getByRole('button', { name: 'Editar' })).toBeDisabled();
    });

    it('separa las descargas de giras de la solicitud de horas extra', async () => {
        granted.permissions = ['EXPORTAR_GIRAS'];
        const { unmount } = renderDraftPage();

        await userEvent.click(await screen.findByRole('button', { name: 'Opciones' }));
        await userEvent.click(screen.getByRole('menuitem', { name: 'Descargar' }));
        expect(await screen.findByRole('menuitem', { name: /Control de Giras/ })).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: /Solicitud de Horas Extra/ })).toBeNull();
        unmount();

        granted.permissions = ['LEER_GIRAS', 'LEER_CHOFERES'];
        renderDraftPage();

        await userEvent.click(await screen.findByRole('button', { name: 'Opciones' }));
        await userEvent.click(screen.getByRole('menuitem', { name: 'Descargar' }));
        expect(await screen.findByRole('menuitem', { name: /Solicitud de Horas Extra/ })).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: /Control de Giras/ })).toBeNull();
    });
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
        expect(previewTourProcessingDraftApprovalWarnings).not.toHaveBeenCalled();
    });

    it('muestra el ícono de problemas solo en las giras con problemas y las coincidencias junto a la observación', async () => {
        fetchTourProcessingDraftRowProblems.mockResolvedValueOnce({
            'row-1': [{ title: 'Vehículos no disponibles asignados a giras', detail: 'Gira 100: vehículo 301-295 no está disponible' }],
        });
        fetchTourProcessingDraftRows.mockReset();
        fetchTourProcessingDraftRows.mockResolvedValueOnce({
            content: [
                { id: 'row-1', number: '100', approvalStatus: 'APPROVED', observations: 'SE REQUIERE chofer', matchedObservationKeywords: ['requiere'], vehicle: '301-295' },
                { id: 'row-2', number: '200', approvalStatus: 'APPROVED', observations: 'Sin novedad', matchedObservationKeywords: [] },
            ],
            totalElements: 2,
        });
        fetchTourProcessingDraftRows.mockImplementation(() => new Promise(() => {}));
        renderDraftPage();

        const firstNumber = await screen.findByTestId('cell-number-row-1');
        expect(firstNumber).toHaveTextContent('100');
        expect(await within(firstNumber).findByRole('button', { name: /Problemas de la gira: Vehículos no disponibles asignados a giras/ })).toBeInTheDocument();
        expect(within(screen.getByTestId('cell-number-row-2')).queryByRole('button')).toBeNull();
        expect(screen.queryByText('No disponible')).toBeNull();

        const firstObservation = screen.getByTestId('cell-observations-row-1');
        expect(within(firstObservation).getByRole('button', { name: 'Coincidencias en la observación: requiere' })).toBeInTheDocument();
        expect(within(screen.getByTestId('cell-observations-row-2')).queryByRole('button')).toBeNull();
    });

    it('avisa al aprobar y guarda solo si se confirma', async () => {
        previewTourProcessingDraftApprovalWarnings.mockResolvedValue([
            { code: 'DRIVER_OVERLAP', title: 'Choferes con giras que se cruzan', message: 'Ana: giras 100 y 200 (04/05/2026)' },
        ]);
        fetchTourProcessingDraftRows.mockReset();
        fetchTourProcessingDraftRows.mockResolvedValueOnce({
            content: [{ id: 'row-3', number: '300', approvalStatus: 'NOT_APPROVED' }],
            totalElements: 1,
        });
        fetchTourProcessingDraftRows.mockImplementation(() => new Promise(() => {}));
        renderDraftPage();

        await userEvent.click(await screen.findByRole('button', { name: 'Aprobar' }));

        expect(await screen.findByText('Esta gira rompe algunas reglas')).toBeInTheDocument();
        expect(screen.getByText(/Ana: giras 100 y 200/)).toBeInTheDocument();
        expect(updateTourProcessingDraftRowApprovalStatus).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
        expect(updateTourProcessingDraftRowApprovalStatus).not.toHaveBeenCalled();
        await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Esta gira rompe algunas reglas' })).not.toBeInTheDocument());

        await userEvent.click(screen.getByRole('button', { name: 'Aprobar' }));
        await userEvent.click(await screen.findByRole('button', { name: 'Guardar de todos modos' }));

        await waitFor(() => expect(updateTourProcessingDraftRowApprovalStatus).toHaveBeenCalledWith('draft-1', 'row-3', 'APPROVED'));
    });
});

describe('comisión final y errores', () => {
    it('descarga la comisión final sin diálogo y sin cambiar estados', async () => {
        granted.permissions = ['EXPORTAR_GIRAS'];
        exportTourProcessingDraftFinalCommission.mockResolvedValue({ blob: new Blob(['comision']), fileName: 'comision.xlsx' });
        renderDraftPage();

        await userEvent.click(await screen.findByRole('button', { name: 'Opciones' }));
        await userEvent.click(screen.getByRole('menuitem', { name: 'Descargar' }));
        await userEvent.click(await screen.findByRole('menuitem', { name: /Comisión Final/ }));

        await waitFor(() => expect(exportTourProcessingDraftFinalCommission).toHaveBeenCalledWith('draft-1'));
        expect(checkTourProcessingDraftFinalCommission).not.toHaveBeenCalled();
        expect(rejectTourProcessingDraftIncompleteTours).not.toHaveBeenCalled();
        expect(screen.queryByRole('dialog')).toBeNull();
    });

    it('muestra los errores y pasa las giras sin chofer o vehículo a No aprobada si se confirma', async () => {
        granted.permissions = ['EDITAR_GIRAS', 'EXPORTAR_GIRAS'];
        checkTourProcessingDraftFinalCommission.mockResolvedValue({
            blockers: [],
            errors: [],
            warnings: [],
            rejections: {
                code: 'NOT_APPROVED',
                title: 'Giras sin chofer o sin vehículo',
                details: ['Gira 100 (04/05/2026), prioridad 1, 3 pasajeros: sin chofer'],
                tours: [{ id: 'row-1', number: '100', label: 'Gira 100 (04/05/2026), prioridad 1, 3 pasajeros: sin chofer' }],
            },
        });
        rejectTourProcessingDraftIncompleteTours.mockResolvedValue(1);
        renderDraftPage();

        await userEvent.click(await screen.findByRole('button', { name: 'Opciones' }));
        await userEvent.click(screen.getByRole('menuitem', { name: 'Visualizar errores' }));

        expect(await screen.findByText('Giras sin atender (1)')).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Gira 100 (04/05/2026), prioridad 1, 3 pasajeros: sin chofer' }));
        await waitFor(() => expect(fetchTourProcessingDraftRow).toHaveBeenCalledWith('draft-1', 'row-1'));

        await userEvent.click(screen.getByRole('button', { name: 'Pasar las giras sin chofer o vehículo a No aprobada' }));
        expect(rejectTourProcessingDraftIncompleteTours).not.toHaveBeenCalled();
        await userEvent.click(screen.getByRole('button', { name: 'Sí, pasar a No aprobada' }));
        await waitFor(() => expect(rejectTourProcessingDraftIncompleteTours).toHaveBeenCalledWith('draft-1'));
    });

    it('guarda el filtro elegido y lo recupera al recargar la página, y descarta valores que ya no existen', async () => {
        granted.permissions = ['LEER_GIRAS'];
        const { unmount } = renderDraftPage();

        await userEvent.click(await screen.findByRole('combobox', { name: /Filtros al mostrar giras/ }));
        await userEvent.click(await screen.findByRole('option', { name: 'Presentan algún problema' }));
        expect(localStorage.getItem('tour-processing-draft-rows-rule')).toBe('ANY');
        unmount();

        fetchTourProcessingDraftRows.mockClear();
        renderDraftPage();
        await waitFor(() => expect(fetchTourProcessingDraftRows).toHaveBeenCalledWith('draft-1', expect.objectContaining({ rule: 'ANY' })));
        expect(await screen.findByRole('combobox', { name: /Filtros al mostrar giras/ })).toHaveTextContent('Presentan algún problema');
        cleanup();

        localStorage.setItem('tour-processing-draft-rows-rule', 'PLATE_REPEATED_SAME_DAY');
        fetchTourProcessingDraftRows.mockClear();
        renderDraftPage();
        await waitFor(() => expect(fetchTourProcessingDraftRows).toHaveBeenCalledWith('draft-1', expect.objectContaining({ rule: '' })));
        expect(await screen.findByRole('combobox', { name: /Filtros al mostrar giras/ })).toHaveTextContent('Mostrar todas');
    });

    it('filtra en el servidor por una condición y por las giras que necesitan más de un viaje', async () => {
        granted.permissions = ['LEER_GIRAS'];
        renderDraftPage();

        await userEvent.click(await screen.findByRole('combobox', { name: /Filtros al mostrar giras/ }));
        await userEvent.click(await screen.findByRole('option', { name: 'Presentan algún problema' }));
        await waitFor(() => expect(fetchTourProcessingDraftRows).toHaveBeenCalledWith('draft-1', expect.objectContaining({ rule: 'ANY' })));

        await userEvent.click(screen.getByRole('combobox', { name: /Filtros al mostrar giras/ }));
        await userEvent.click(await screen.findByRole('option', { name: 'Más de un viaje y sin atender' }));
        await waitFor(() => expect(fetchTourProcessingDraftRows).toHaveBeenCalledWith('draft-1', expect.objectContaining({ rule: 'NEEDS_MULTIPLE_TRIPS' })));
    });
});
