import { useQuery } from '@tanstack/react-query';
import { fetchVehicleMaintenance } from '../services/maintenance/maintenanceService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { maintenanceStatusLabel } from '../transportUtils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapMaintenanceToRow(record) {
    return {
        id: record.id,
        vehicleId: record.vehicle?.id ?? record.vehicleId ?? '',
        vehiclePlate: record.vehicle?.plate ?? record.vehiclePlate ?? '—',
        title: record.title ?? record.description ?? '—',
        type: record.type ?? '—',
        scheduledDate: record.scheduledDate ?? null,
        cost: record.cost ?? null,
        status: maintenanceStatusLabel(record.status),
        statusRaw: record.status ?? '',
        createdAt: record.createdAt ?? null,
        updatedAt: record.updatedAt ?? null,
        notes: record.notes ?? '',
    };
}

export function useTransportMaintenanceData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.transport.maintenanceList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchVehicleMaintenance(requestParams);
            return {
                rows: (response.content ?? []).map(mapMaintenanceToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar mantenimientos') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
