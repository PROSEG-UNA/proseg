import { useQuery } from '@tanstack/react-query';
import { fetchAssets } from '../services/assetsService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapStatusToSpanish(status) {
    switch (status) {
        case 'APROBADO': return 'Aprobado';
        case 'DE_BAJA': return 'De baja';
        default: return status || '—';
    }
}

function hasDetailContent(asset) {
    if (asset.componentsCount == null) return true;
    return Boolean(asset.networkInterface) || asset.imagesCount > 0 || asset.componentsCount > 0;
}

function mapAssetToRow(asset) {
    const statusRaw = asset.status || '';
    return {
        id: asset.id,
        kind: asset.kind || '—',
        assetNumber: asset.assetNumber ?? '—',
        serialNumber: asset.serialNumber ?? '—',
        executingUnit: asset.executingUnit || '—',
        responsibleEmployee: asset.responsibleEmployee || '—',
        responsibleEmployeeId: asset.responsibleEmployeeId || '—',
        brand: asset.model?.brand?.name || '—',
        model: asset.model?.name || '—',
        type: asset.model?.type?.name || '—',
        location: asset.location?.description || '—',
        campus: asset.location?.floor?.building?.campus?.name || '—',
        status: mapStatusToSpanish(statusRaw),
        statusRaw,
        acquisitionDate: asset.acquisitionDate || null,
        warrantyEndDate: asset.warrantyEndDate || null,
        firmwareSupportEndDate: asset.firmwareSupportEndDate || null,
        decommissionDate: asset.decommissionDate || null,
        latitude: asset.latitude ?? null,
        longitude: asset.longitude ?? null,
        hasDetailContent: hasDetailContent(asset),
    };
}

export function useAssetsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.inventory.assetList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchAssets(requestParams);
            return {
                rows: (response.content ?? []).map(mapAssetToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar activos') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
