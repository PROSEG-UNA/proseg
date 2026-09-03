import { useQuery } from '@tanstack/react-query';
import { fetchTours } from '../services/tours/toursService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { tourStatusLabel } from '../transportUtils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapTourToRow(tour) {
    return {
        id: tour.id,
        name: tour.name ?? '—',
        origin: tour.origin ?? '—',
        destination: tour.destination ?? '—',
        startDate: tour.startDate ?? null,
        endDate: tour.endDate ?? null,
        driverId: tour.driver?.id ?? tour.driverId ?? '',
        driverName: [tour.driver?.firstName, tour.driver?.lastName].filter(Boolean).join(' ').trim() || tour.driverName || '—',
        vehicleId: tour.vehicle?.id ?? tour.vehicleId ?? '',
        vehiclePlate: tour.vehicle?.plate ?? tour.vehiclePlate ?? '—',
        status: tourStatusLabel(tour.status),
        statusRaw: tour.status ?? '',
        createdAt: tour.createdAt ?? null,
        updatedAt: tour.updatedAt ?? null,
    };
}

export function useTransportToursData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.transport.tourList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchTours(requestParams);
            return {
                rows: (response.content ?? []).map(mapTourToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar giras') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
