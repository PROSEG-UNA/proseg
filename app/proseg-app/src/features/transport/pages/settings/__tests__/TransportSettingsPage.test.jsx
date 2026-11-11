import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import { PERMISSIONS } from '../../../../../common/constants/permissions';
import {
    fetchTourSplitKeywords,
    fetchTourSplitSettings,
    updateTourSplitSettings,
} from '../../../services/tourSplit/tourSplitService';
import TransportSettingsPage from '../TransportSettingsPage.jsx';

const granted = vi.hoisted(() => ({ permissions: [] }));

vi.mock('../../../../../common/hooks/index.js', () => ({
    usePermissions: () => ({
        hasPermission: (permission) => granted.permissions.includes(permission),
        hasAnyPermission: (permissions) => permissions.some((permission) => granted.permissions.includes(permission)),
    }),
}));

vi.mock('../../../../../common/hooks/usePermissions', () => ({
    usePermissions: () => ({
        hasPermission: (permission) => granted.permissions.includes(permission),
        hasAnyPermission: (permissions) => permissions.some((permission) => granted.permissions.includes(permission)),
    }),
}));

vi.mock('../../../services/observationKeywords/observationKeywordsService', () => ({
    fetchObservationKeywords: vi.fn().mockResolvedValue({ content: [], totalPages: 0 }),
}));

vi.mock('../../../services/tourSplit/tourSplitService', () => ({
    TOUR_SPLIT_MODES: { DISTINCT: 'DISTINCT_VEHICLES', REPEATING: 'SAME_DRIVER_AND_VEHICLE' },
    fetchTourSplitKeywords: vi.fn(),
    fetchTourSplitSettings: vi.fn(),
    updateTourSplitSettings: vi.fn(),
    fetchTourSplitKeywordById: vi.fn(),
    createTourSplitKeyword: vi.fn(),
    updateTourSplitKeyword: vi.fn(),
    deleteTourSplitKeyword: vi.fn(),
}));

const DISTINCT_SWITCH = 'Resto de destinos: choferes distintos';
const REPEATING_SWITCH = 'Resto de destinos: repiten viajes';

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <AppTheme defaultColorScheme="light">
            <QueryClientProvider client={queryClient}>
                <TransportSettingsPage />
            </QueryClientProvider>
        </AppTheme>,
    );
}

beforeEach(() => {
    granted.permissions = [PERMISSIONS.TRANSPORT.TOURS.READ, PERMISSIONS.TRANSPORT.TOURS.UPDATE];
    fetchTourSplitKeywords.mockImplementation(async (mode) => (mode === 'DISTINCT_VEHICLES'
        ? [{ id: 'k1', text: 'Cartago', mode }]
        : [{ id: 'k2', text: 'San José', mode }]));
    fetchTourSplitSettings.mockResolvedValue({ defaultMode: 'DISTINCT_VEHICLES' });
    updateTourSplitSettings.mockImplementation(async (payload) => payload);
});

afterEach(() => {
    cleanup();
    vi.clearAllMocks();
});

describe('bancos de palabras para dividir giras', () => {
    it('muestra cada banco con sus palabras y el interruptor de choferes distintos activado por defecto', async () => {
        renderPage();

        const distinctBank = await screen.findByRole('group', { name: /Destinos de choferes distintos/ });
        const repeatingBank = await screen.findByRole('group', { name: /Destinos de choferes que repiten viajes/ });
        expect(await within(distinctBank).findByText('Cartago')).toBeTruthy();
        expect(await within(repeatingBank).findByText('San José')).toBeTruthy();
        await waitFor(() => expect(screen.getByRole('switch', { name: DISTINCT_SWITCH }).checked).toBe(true));
        expect(screen.getByRole('switch', { name: REPEATING_SWITCH }).checked).toBe(false);
    });

    it('al activar el interruptor de repetir viajes desactiva el de choferes distintos', async () => {
        fetchTourSplitSettings
            .mockResolvedValueOnce({ defaultMode: 'DISTINCT_VEHICLES' })
            .mockResolvedValue({ defaultMode: 'SAME_DRIVER_AND_VEHICLE' });
        renderPage();
        await waitFor(() => expect(screen.getByRole('switch', { name: DISTINCT_SWITCH }).checked).toBe(true));

        await userEvent.click(screen.getByRole('switch', { name: REPEATING_SWITCH }));

        await waitFor(() => expect(updateTourSplitSettings).toHaveBeenCalledWith({ defaultMode: 'SAME_DRIVER_AND_VEHICLE' }));
        await waitFor(() => expect(screen.getByRole('switch', { name: REPEATING_SWITCH }).checked).toBe(true));
        expect(screen.getByRole('switch', { name: DISTINCT_SWITCH }).checked).toBe(false);
    });

    it('al apagar el interruptor activo enciende el del otro banco', async () => {
        renderPage();
        await waitFor(() => expect(screen.getByRole('switch', { name: DISTINCT_SWITCH }).checked).toBe(true));

        await userEvent.click(screen.getByRole('switch', { name: DISTINCT_SWITCH }));

        await waitFor(() => expect(updateTourSplitSettings).toHaveBeenCalledWith({ defaultMode: 'SAME_DRIVER_AND_VEHICLE' }));
    });
});
