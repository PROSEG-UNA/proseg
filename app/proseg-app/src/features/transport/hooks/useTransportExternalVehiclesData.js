import { useQuery } from '@tanstack/react-query';
import { fetchExternalVehicles } from '../services/externalVehicles/externalVehiclesService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapExternalVehicleToRow(vehicle) {
    return {
        id: vehicle.id,
        provider: vehicle.provider ?? '—',
        plate: vehicle.plate ?? '—',
        capacity: vehicle.capacity ?? '—',
        available: Boolean(vehicle.available),
    };
}

export function useTransportExternalVehiclesData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.transport.externalVehicleList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchExternalVehicles(requestParams);
            return {
                rows: (response.content ?? []).map(mapExternalVehicleToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar la contratación externa') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
