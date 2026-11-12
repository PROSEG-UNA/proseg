import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchObservationKeywords } from '../../services/observationKeywords/observationKeywordsService';
import { useObservationKeywordsData } from '../useObservationKeywordsData';

vi.mock('../../services/observationKeywords/observationKeywordsService', () => ({
    fetchObservationKeywords: vi.fn(),
}));

describe('palabras clave en configuraciones', () => {
    beforeEach(() => {
        fetchObservationKeywords.mockReset();
    });

    it('muestra también las palabras que vienen después de la primera página', async () => {
        fetchObservationKeywords
            .mockResolvedValueOnce({ content: [{ id: '1', text: 'Urgente' }], totalPages: 2 })
            .mockResolvedValueOnce({ content: [{ id: '2', text: 'Revisar' }], totalPages: 2 });
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

        const { result } = renderHook(() => useObservationKeywordsData(), {
            wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
        });

        await waitFor(() => expect(result.current.rows).toEqual([
            { id: '1', text: 'Urgente' },
            { id: '2', text: 'Revisar' },
        ]));
        expect(fetchObservationKeywords).toHaveBeenNthCalledWith(1, { page: 0, size: 100, sort: ['text,asc'] });
        expect(fetchObservationKeywords).toHaveBeenNthCalledWith(2, { page: 1, size: 100, sort: ['text,asc'] });
    });
});
