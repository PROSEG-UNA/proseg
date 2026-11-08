import { useQuery } from '@tanstack/react-query';
import { fetchBorrowedVehicles } from '../services/borrowedVehicles/borrowedVehiclesService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapBorrowedVehicleToRow(vehicle) {
    return {
        id: vehicle.id,
        plate: vehicle.plate ?? '—',
        type: vehicle.type ?? '',
        capacity: vehicle.capacity ?? '—',
    };
}

export function useTransportBorrowedVehiclesData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.transport.borrowedVehicleList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchBorrowedVehicles(requestParams);
            return {
                rows: (response.content ?? []).map(mapBorrowedVehicleToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar vehículos prestados') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
