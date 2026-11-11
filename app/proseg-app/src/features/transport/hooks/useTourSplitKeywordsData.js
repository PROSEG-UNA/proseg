import { useQuery } from '@tanstack/react-query';
import { fetchTourSplitKeywords } from '../services/tourSplit/tourSplitService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { queryKeys } from '../../../common/query';

export function useTourSplitKeywordsData(mode) {
    const { data, isLoading, isFetching, error, refetch } = useQuery({
        queryKey: queryKeys.transport.tourSplitKeywordList(mode),
        queryFn: () => fetchTourSplitKeywords(mode),
    });

    return {
        rows: data ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar los destinos') : null,
        refetch,
    };
}
