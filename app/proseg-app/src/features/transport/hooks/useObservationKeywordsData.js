import { useQuery } from '@tanstack/react-query';
import { fetchObservationKeywords } from '../services/observationKeywords/observationKeywordsService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapKeywordToRow(keyword) {
    return {
        id: keyword.id,
        text: keyword.text ?? '',
    };
}

export function useObservationKeywordsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };
    const listQueryKey = queryKeys.transport.observationKeywordList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchObservationKeywords(requestParams);
            return {
                rows: (response.content ?? []).map(mapKeywordToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar las palabras clave') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
