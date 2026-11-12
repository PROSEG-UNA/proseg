import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingDraftPage from '../TourProcessingDraftPage.jsx';
import { checkTourProcessingDraftFinalCommission, exportTourProcessingDraftFinalCommission, fetchTourProcessingDraftRow, fetchTourProcessingDraftRowProblems, fetchTourProcessingDraftRows, finishTourProcessingDraft, rejectTourProcessingDraftIncompleteTours, reopenTourProcessingDraft } from '../../../services/tourProcessing/tourProcessingService.js';
import { queryKeys } from '../../../../../common/query';

const granted = vi.hoisted(() => ({ permissions: null }));

vi.mock('../../../../../common/hooks/index.js', () => ({
    usePermissions: () => ({
        hasAnyPermission: (permissions) => granted.permissions === null
            || permissions.some((permission) => granted.permissions.includes(permission)),
    }),
}));

vi.mock('../../../../../common/components/TablaBase.jsx', () => ({
    default: ({ data, columns, onRowClick }) => <div>{data.map((item) => (
        <section key={item.id} data-testid={`row-${item.id}`} onClick={() => onRowClick?.({ original: item })}>
            <span>{item.approvalStatus}</span>
            {columns.filter((column) => column.accessorKey === 'number' || column.accessorKey === 'observations').map((column) => (
                <div key={column.id ?? column.accessorKey} data-testid={`cell-${column.id ?? column.accessorKey}-${item.id}`}>
                    {column.Cell({
                        cell: { getValue: () => item[column.accessorKey] },
                        row: { original: item },
                        table: { getState: () => ({ isFullScreen: false }) },
                    })}
                </div>
            ))}
        </section>
    ))}</div>,
}));

vi.mock('../../../components/tourProcessing/TourProcessingDraftRowModal.jsx', () => ({
    default: ({ open, mode, row }) => (open ? <div data-testid="row-modal">{`${mode}:${row?.id ?? ''}`}</div> : null),
}));
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
        await userEvent.click(closedRow);
        expect(await screen.findByTestId('row-modal')).toHaveTextContent('view:row-1');
        await userEvent.click(screen.getByRole('button', { name: 'Opciones' }));
        await userEvent.click(screen.getByRole('menuitem', { name: 'Devolver a borrador' }));
        await userEvent.click(screen.getByRole('button', { name: 'Devolver a borrador' }));

        await waitFor(() => expect(reopenTourProcessingDraft).toHaveBeenCalledWith('draft-1'));
        await waitFor(() => expect(queryClient.getQueryData(queryKeys.transport.tourProcessingDraftDetail('draft-1')).status).toBe('DRAFT'));
        await waitFor(async () => {
            await userEvent.click(closedRow);
            expect(screen.getByTestId('row-modal')).toHaveTextContent('edit:row-1');
        });
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
        await userEvent.click(row);
        expect(await screen.findByTestId('row-modal')).toHaveTextContent('view:row-1');
        expect(screen.queryByRole('button', { name: 'Opciones' })).toBeNull();
    });

    it('con CREAR_GIRAS permite crear pero abre la fila solo para ver', async () => {
        granted.permissions = ['CREAR_GIRAS'];
        renderDraftPage();

        const row = await screen.findByTestId('row-row-1');
        await userEvent.click(row);
        expect(await screen.findByTestId('row-modal')).toHaveTextContent('view:row-1');
        expect(await openOptions()).toEqual(['Crear gira']);
    });

    it('con EDITAR_GIRAS abre la fila para editar al hacer clic', async () => {
        granted.permissions = ['EDITAR_GIRAS'];
        renderDraftPage();

        const row = await screen.findByTestId('row-row-1');
        await userEvent.click(row);
        expect(await screen.findByTestId('row-modal')).toHaveTextContent('edit:row-1');
        const options = await openOptions();
        expect(options).toContain('Terminar depuración');
        expect(options).not.toContain('Crear gira');
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

describe('columnas de la tabla', () => {
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
