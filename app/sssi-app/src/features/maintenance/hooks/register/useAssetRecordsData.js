import { useQuery } from '@tanstack/react-query';
import { fetchAssetRecords } from '../../services/register/registerService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../../common/query';

function mapRecordToRow(record) {
    return {
        id: record.id,
        assetId: record.assetId,
        companyName: record.company?.name ?? '—',
        userEmail: record.userEmail ?? '—',
        description: record.description ?? '—',
        createdAt: record.createdAt ?? null,
    };
}

export function useAssetRecordsData({ registerId = null, assetId = null, pageIndex = 0, pageSize = 10 } = {}) {
    const requestParams = { assetId, page: pageIndex, size: pageSize };

    const listQueryKey = queryKeys.maintenance.assetRecords(registerId, assetId, { page: pageIndex, size: pageSize });

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchAssetRecords(registerId, requestParams);
            return {
                rows: (response.content ?? []).map(mapRecordToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
        enabled: Boolean(registerId && assetId),
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar el historial del activo') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
