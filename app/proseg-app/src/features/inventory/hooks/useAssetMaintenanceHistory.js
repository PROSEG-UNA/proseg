import { useQuery } from '@tanstack/react-query';
import { fetchAssetMaintenanceHistory } from '../services/maintenanceHistoryService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapHistoryRecordToRow(record) {
    return {
        id: record.id,
        assetId: record.assetId,
        companyName: record.company?.name ?? '—',
        userEmail: record.userEmail ?? '—',
        description: record.description ?? '—',
        createdAt: record.createdAt ?? null,
    };
}

export function useAssetMaintenanceHistory({ assetId = null, pageIndex = 0, pageSize = 10 } = {}) {
    const listQueryKey = queryKeys.inventory.assetMaintenanceHistory(assetId, { page: pageIndex, size: pageSize });

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchAssetMaintenanceHistory({ assetId, page: pageIndex, size: pageSize });
            return {
                rows: (response.content ?? []).map(mapHistoryRecordToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
        enabled: Boolean(assetId),
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar el historial de mantenimiento') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
