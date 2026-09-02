import { useQuery } from '@tanstack/react-query';
import { fetchCatalogPage } from '../services/catalogService';
import { keepPreviousPage, queryKeys } from '../../../common/query';

export function useCatalogData({ baseUrl, pageIndex, pageSize, search = '', filters = {}, sort = [] }) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.inventory.catalogList(baseUrl, requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: () => fetchCatalogPage(baseUrl, requestParams),
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
