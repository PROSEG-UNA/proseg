import { useQuery } from '@tanstack/react-query';
import { fetchAssetDashboard, fetchDashboardAssets } from '../services/assetDashboardService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';
import { mapAssetToRow } from './useAssetsData';

export function useAssetDashboard(days) {
    const dashboardQueryKey = queryKeys.inventory.assetDashboard(days);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: dashboardQueryKey,
        queryFn: () => fetchAssetDashboard(days),
    });

    return {
        summary: data ?? null,
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar el panel de activos') : null,
    };
}

export function useDashboardAssets({ category, days, pageIndex = 0, pageSize = 10 } = {}) {
    const requestParams = { category, days, page: pageIndex, size: pageSize };
    const listQueryKey = queryKeys.inventory.assetDashboardList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        enabled: Boolean(category),
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchDashboardAssets(requestParams);
            return {
                rows: (response.content ?? []).map(mapAssetToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        totalElements: data?.totalElements ?? 0,
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar activos') : null,
    };
}
