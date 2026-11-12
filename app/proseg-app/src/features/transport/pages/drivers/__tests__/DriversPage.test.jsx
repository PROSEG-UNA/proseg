import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import DriversPage from '../DriversPage.jsx';
import { exportMonthTourControl } from '../../../services/tourProcessing/tourProcessingService';
import { exportDrivers } from '../../../services/transportExportService.js';
import { downloadBlob } from '../../../transportUtils.js';

const granted = vi.hoisted(() => ({ permissions: [] }));

vi.mock('../../../../../common/hooks/index.js', () => ({
    usePermissions: () => ({
        hasPermission: (permission) => granted.permissions.includes(permission),
        hasAnyPermission: (permissions) => permissions.some((permission) => granted.permissions.includes(permission)),
    }),
}));

vi.mock('../../../../../common/components/index.js', async (importOriginal) => ({
    ...(await importOriginal()),
    MonthPicker: () => null,
}));

const tableProps = vi.hoisted(() => ({ latest: null }));

vi.mock('../../../components/drivers/DriversTable.jsx', () => ({
    default: (props) => {
        tableProps.latest = props;
        return null;
    },
}));
vi.mock('../../../components/drivers/DriverFormModal.jsx', () => ({ default: () => null }));
vi.mock('../../../components/drivers/DriverSurplusSummary.jsx', () => ({ default: () => null }));
vi.mock('../../../services/drivers/driversService', () => ({ exportDriverOvertimeRequest: vi.fn() }));
vi.mock('../../../services/tourProcessing/tourProcessingService', () => ({ exportMonthTourControl: vi.fn() }));
vi.mock('../../../services/transportExportService.js', () => ({ exportDrivers: vi.fn() }));
vi.mock('../../../transportUtils.js', async (importOriginal) => ({
    ...(await importOriginal()),
    downloadBlob: vi.fn(),
}));

vi.mock('../../../hooks/usePersistedMonth.js', () => ({
    usePersistedMonth: () => [dayjs('2026-05-01'), vi.fn()],
}));

const monthDrivers = vi.hoisted(() => ({
    byDriverId: new Map(),
}));

vi.mock('../../../hooks/useTransportDriverMonthsData.js', () => ({
    useTransportDriverMonthsData: () => ({
        enabled: true,
        loading: false,
        hasAnyRecord: true,
        byDriverId: monthDrivers.byDriverId,
    }),
}));

vi.mock('../../../hooks/useTransportDriverMonthSurplusCap.js', () => ({
    useTransportDriverMonthSurplusCap: () => ({ surplusHoursPerDriver: null }),
}));

const locationProbe = vi.hoisted(() => ({ search: '' }));

function LocationProbe() {
    locationProbe.search = useLocation().search;
    return null;
}

const renderPage = (initialEntry = '/transporte/choferes') => render(
    <AppTheme defaultColorScheme="light">
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <DriversPage />
                <LocationProbe />
            </MemoryRouter>
        </QueryClientProvider>
    </AppTheme>,
);

beforeEach(() => {
    granted.permissions = [];
    localStorage.clear();
    tableProps.latest = null;
    monthDrivers.byDriverId = new Map();
});

afterEach(cleanup);

describe('pestaña en la ruta', () => {
    beforeEach(() => {
        granted.permissions = ['LEER_CHOFERES'];
    });

    it('completa la ruta con servicios-generales cuando no trae pestaña', async () => {
        renderPage();

        await waitFor(() => expect(locationProbe.search).toBe('?tab=servicios-generales'));
        expect(tableProps.latest.unit).toBe('GENERAL_SERVICES');
    });

    it('abre la pestaña de otra unidad desde la ruta', () => {
        renderPage('/transporte/choferes?tab=otra-unidad');

        expect(tableProps.latest.unit).toBe('OTHER_UNIT');
        expect(locationProbe.search).toBe('?tab=otra-unidad');
    });

    it('cambia la ruta al elegir otra pestaña', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('tab', { name: /Choferes de otra unidad/ }));

        expect(locationProbe.search).toBe('?tab=otra-unidad');
        expect(tableProps.latest.unit).toBe('OTHER_UNIT');
    });

    it('corrige una pestaña inválida', async () => {
        renderPage('/transporte/choferes?tab=cualquiera');

        await waitFor(() => expect(locationProbe.search).toBe('?tab=servicios-generales'));
    });
});

describe('filtro de choferes que participaron en el mes', () => {
    beforeEach(() => {
        granted.permissions = ['LEER_CHOFERES'];
        monthDrivers.byDriverId = new Map([
            ['b', { driverId: 'b', ordinaryHoursAccrued: 0, surplusHoursAccrued: 2, overtimeHoursAccrued: 0, weekendsWorked: 0 }],
            ['a', { driverId: 'a', ordinaryHoursAccrued: 8, surplusHoursAccrued: 0, overtimeHoursAccrued: 0, weekendsWorked: 0 }],
            ['c', { driverId: 'c', ordinaryHoursAccrued: 0, surplusHoursAccrued: 0, overtimeHoursAccrued: 0, weekendsWorked: 0 }],
        ]);
    });

    it('por defecto muestra solo los que participaron en otra unidad', () => {
        renderPage('/transporte/choferes?tab=otra-unidad');

        expect(screen.getByRole('combobox', { name: 'Mostrar' })).toHaveTextContent('Choferes que participaron en el mes');
        expect(tableProps.latest.participantIds).toEqual(['a', 'b']);
    });

    it('no aparece en la pestaña de la unidad', () => {
        renderPage('/transporte/choferes?tab=servicios-generales');

        expect(screen.queryByRole('combobox', { name: 'Mostrar' })).toBeNull();
        expect(tableProps.latest.participantIds).toBeNull();
    });

    it('al elegir Todos deja de restringir y lo recuerda en localStorage', async () => {
        renderPage('/transporte/choferes?tab=otra-unidad');

        await userEvent.click(screen.getByRole('combobox', { name: 'Mostrar' }));
        await userEvent.click(screen.getByRole('option', { name: 'Todos' }));

        expect(tableProps.latest.participantIds).toBeNull();
        expect(localStorage.getItem('transport-drivers-participation-filter')).toBe('all');
    });

    it('arranca con Todos si así quedó guardado', () => {
        localStorage.setItem('transport-drivers-participation-filter', 'all');
        renderPage('/transporte/choferes?tab=otra-unidad');

        expect(screen.getByRole('combobox', { name: 'Mostrar' })).toHaveTextContent('Todos');
        expect(tableProps.latest.participantIds).toBeNull();
    });
});

describe('Control de Giras del mes', () => {
    it('descarga el archivo del mes seleccionado con EXPORTAR_GIRAS', async () => {
        const file = { blob: new Blob(['control']), fileName: 'Control de Giras Mayo 26.xlsx' };
        exportMonthTourControl.mockResolvedValue(file);
        granted.permissions = ['LEER_CHOFERES', 'EXPORTAR_GIRAS'];
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Opciones' }));
        await userEvent.click(screen.getByRole('menuitem', { name: /Control de Giras/ }));

        expect(exportMonthTourControl).toHaveBeenCalledWith(2026, 5);
        await waitFor(() => expect(downloadBlob).toHaveBeenCalledWith(file.blob, file.fileName));
    });

    it('no la ofrece sin EXPORTAR_GIRAS', async () => {
        granted.permissions = ['LEER_CHOFERES'];
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Opciones' }));

        expect(screen.queryByRole('menuitem', { name: /Control de Giras/ })).toBeNull();
    });
});

describe('permisos de la Solicitud de Horas Extra', () => {
    it('la ofrece con LEER_CHOFERES', async () => {
        granted.permissions = ['LEER_CHOFERES'];
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Opciones' }));

        expect(screen.getByRole('menuitem', { name: /Solicitud de Horas Extra/ })).toBeInTheDocument();
    });

    it('no la ofrece solo con GESTIONAR_CHOFERES', async () => {
        granted.permissions = ['GESTIONAR_CHOFERES'];
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Opciones' }));

        expect(screen.getByRole('menuitem', { name: 'Crear chofer' })).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: /Solicitud de Horas Extra/ })).toBeNull();
        expect(screen.queryByRole('menuitem', { name: 'Exportar choferes' })).toBeNull();
    });
});

describe('exportar choferes', () => {
    it('descarga el excel del mes que se ve en pantalla con LEER_CHOFERES', async () => {
        const file = { blob: new Blob(['choferes']), fileName: 'choferes.xlsx' };
        exportDrivers.mockResolvedValue(file);
        granted.permissions = ['LEER_CHOFERES'];
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Opciones' }));
        await userEvent.click(screen.getByRole('menuitem', { name: 'Exportar choferes' }));

        expect(exportDrivers).toHaveBeenCalledWith({ year: 2026, month: 5 });
        await waitFor(() => expect(downloadBlob).toHaveBeenCalledWith(file.blob, file.fileName));
    });
});
