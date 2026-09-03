import { useQuery } from '@tanstack/react-query';
import { fetchLocationPage } from '../services/locationService.js';
import { keepPreviousPage, queryKeys } from '../../../common/query';

export function useLocationData({ baseUrl, pageIndex, pageSize, search = '', filters = {}, sort = [] }) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.locations.list(baseUrl, requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: () => fetchLocationPage(baseUrl, requestParams),
        enabled: Boolean(baseUrl),
    });

    return {
        rows: data?.content ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? (error?.response?.data?.message ?? error?.message ?? 'Error al cargar datos') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
