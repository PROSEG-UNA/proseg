import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';
import { queryKeys } from '../../../../common/query';
import { fetchTourProcessingDraftRows } from '../../services/tourProcessing/tourProcessingService.js';
import { invalidateTourProcessingDraft, replaceCachedDraftRow, useTourProcessingDraftRows } from '../useTourProcessingDraftsData.js';

vi.mock('../../services/tourProcessing/tourProcessingService.js', () => ({
    fetchTourProcessingDraftRows: vi.fn(),
}));

describe('caché del borrador de transporte', () => {
    it('conserva la forma de las claves existentes', () => {
        const params = { page: 0, size: 10 };
        expect(queryKeys.transport.tourProcessingDraftRows('draft-1', params)).toEqual(['transport', 'tourProcessing', 'drafts', 'draft', 'draft-1', 'rows', params]);
        expect(queryKeys.transport.driverMonths(2026, 5)).toEqual(['transport', 'drivers', 'months', { year: 2026, month: 5 }]);
    });

    it('sustituye solo las filas ya presentes sin cambiar el total', () => {
        const queryClient = new QueryClient();
        const key = queryKeys.transport.tourProcessingDraftRows('draft-1', { page: 0 });
        queryClient.setQueryData(key, { rows: [{ id: 'row-1', observations: 'Antes' }], totalElements: 25 });

        replaceCachedDraftRow(queryClient, 'draft-1', { id: 'row-1', observations: 'Después', departureTime: '07:00:00' });
        expect(queryClient.getQueryData(key)).toEqual({ rows: [{ id: 'row-1', observations: 'Después', departureTime: '07:00', returnTime: null }], totalElements: 25 });

        replaceCachedDraftRow(queryClient, 'draft-1', { id: 'row-2' });
        expect(queryClient.getQueryData(key).rows).toHaveLength(1);
    });

    it('mantiene los datos en caché sin pedir filas cuando está deshabilitado', async () => {
        const queryClient = new QueryClient();
        const params = { page: 0, size: 10, search: '', filters: {}, sort: [] };
        queryClient.setQueryData(queryKeys.transport.tourProcessingDraftRows('draft-1', params), { rows: [{ id: 'row-1' }], totalElements: 1 });
        fetchTourProcessingDraftRows.mockClear();

        const { result } = renderHook(() => useTourProcessingDraftRows('draft-1', {}, { enabled: false }), {
            wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
        });

        await waitFor(() => expect(result.current.rows).toEqual([{ id: 'row-1' }]));
        expect(fetchTourProcessingDraftRows).not.toHaveBeenCalled();
    });

    it('marca como stale los meses de choferes', async () => {
        const queryClient = new QueryClient();
        const key = queryKeys.transport.driverMonths(2026, 5);
        queryClient.setQueryData(key, { content: [] });

        await invalidateTourProcessingDraft(queryClient, 'draft-1');

        expect(queryClient.getQueryState(key).isInvalidated).toBe(true);
    });
});
