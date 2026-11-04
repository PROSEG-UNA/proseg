import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import DriversPage from '../DriversPage.jsx';

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

vi.mock('../../../components/drivers/DriversTable.jsx', () => ({ default: () => null }));
vi.mock('../../../components/drivers/DriverFormModal.jsx', () => ({ default: () => null }));
vi.mock('../../../components/drivers/DriverSurplusSummary.jsx', () => ({ default: () => null }));
vi.mock('../../../services/drivers/driversService', () => ({ exportDriverOvertimeRequest: vi.fn() }));

vi.mock('../../../hooks/usePersistedMonth.js', () => ({
    usePersistedMonth: () => [dayjs('2026-05-01'), vi.fn()],
}));

vi.mock('../../../hooks/useTransportDriverMonthsData.js', () => ({
    useTransportDriverMonthsData: () => ({ enabled: true, loading: false, hasAnyRecord: true }),
}));

vi.mock('../../../hooks/useTransportDriverMonthSurplusCap.js', () => ({
    useTransportDriverMonthSurplusCap: () => ({ surplusHoursPerDriver: null }),
}));

const renderPage = () => render(
    <AppTheme defaultColorScheme="light">
        <QueryClientProvider client={new QueryClient()}>
            <DriversPage />
        </QueryClientProvider>
    </AppTheme>,
);

beforeEach(() => {
    granted.permissions = [];
});

afterEach(cleanup);

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
    });
});
