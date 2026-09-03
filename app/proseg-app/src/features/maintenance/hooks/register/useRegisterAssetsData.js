import { useQuery } from '@tanstack/react-query';
import { fetchRegisterAssets } from '../../services/register/registerService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../../common/query';

function mapStatusToSpanish(status) {
    switch (status) {
        case 'APROBADO': return 'Aprobado';
        case 'DE_BAJA': return 'De baja';
        default: return status || '—';
    }
}

function mapAssetRow(asset) {
    const statusRaw = asset.status || '';
    return {
        id: asset.id,
        kind: asset.kind || '—',
        assetNumber: asset.assetNumber ?? '—',
        serialNumber: asset.serialNumber ?? '—',
        type: asset.type || '—',
        brand: asset.brand || '—',
        model: asset.modelName || '—',
        campus: asset.campusName || '—',
        building: asset.buildingName || '—',
        floor: asset.floorName || '—',
        location: asset.locationName || '—',
        executingUnit: asset.executingUnit || '—',
        responsibleEmployee: asset.responsibleEmployee || '—',
        responsibleEmployeeId: asset.responsibleEmployeeId || '—',
        status: mapStatusToSpanish(statusRaw),
        statusRaw,
        acquisitionDate: asset.acquisitionDate || null,
        warrantyEndDate: asset.warrantyEndDate || null,
        firmwareSupportEndDate: asset.firmwareSupportEndDate || null,
        decommissionDate: asset.decommissionDate || null,
        latitude: asset.latitude ?? null,
        longitude: asset.longitude ?? null,
    };
}

export function useRegisterAssetsData({ registerId = null, pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.maintenance.registerAssets(registerId, requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchRegisterAssets(registerId, requestParams);
            return {
                rows: (response.content ?? []).map(mapAssetRow),
                totalElements: response.totalElements ?? 0,
            };
        },
        enabled: Boolean(registerId),
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar los activos') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
