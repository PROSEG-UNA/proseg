import { useQuery } from '@tanstack/react-query';
import { fetchVehicles } from '../services/vehicles/vehiclesService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapVehicleToRow(vehicle) {
    return {
        id: vehicle.id,
        plate: vehicle.plate ?? '—',
        type: vehicle.type ?? '',
        capacity: vehicle.capacity ?? '—',
        available: Boolean(vehicle.available),
    };
}

export function useTransportVehiclesData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.transport.vehicleList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchVehicles(requestParams);
            return {
                rows: (response.content ?? []).map(mapVehicleToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar vehículos') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
