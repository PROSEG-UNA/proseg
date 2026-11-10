import { useQuery } from '@tanstack/react-query';
import { fetchObservationKeywords } from '../services/observationKeywords/observationKeywordsService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { queryKeys } from '../../../common/query';

function mapKeywordToRow(keyword) {
    return {
        id: keyword.id,
        text: keyword.text ?? '',
    };
}

export function useObservationKeywordsData() {
    const { data, isLoading, isFetching, error, refetch } = useQuery({
        queryKey: queryKeys.transport.observationKeywordList({ all: true }),
        queryFn: async () => {
            const rows = [];
            let page = 0;
            let totalPages;

            do {
                const response = await fetchObservationKeywords({ page, size: 100, sort: ['text,asc'] });
                rows.push(...(response.content ?? []).map(mapKeywordToRow));
                totalPages = response.totalPages ?? 0;
                page += 1;
            } while (page < totalPages);

            return rows;
        },
    });

    return {
        rows: data ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar las palabras clave') : null,
        refetch,
    };
}
