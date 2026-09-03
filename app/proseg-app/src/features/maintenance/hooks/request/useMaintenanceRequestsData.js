import { useQuery } from '@tanstack/react-query';
import { fetchMaintenanceRequests } from '../../services/request/requestsService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { statusLabel } from '../../maintenanceUtils';
import { keepPreviousPage, queryKeys } from '../../../../common/query';

function mapRequestToRow(request) {
    return {
        id: request.id,
        companyId: request.company?.id ?? '',
        companyName: request.company?.name ?? '—',
        companyLegalId: request.company?.legalId ?? '—',
        email: Array.isArray(request.emails) && request.emails.length > 0 ? request.emails.join(', ') : '—',
        description: request.description ?? '—',
        status: statusLabel(request.status),
        statusRaw: request.status ?? '',
        startDate: request.startDate ?? null,
        endDate: request.endDate ?? null,
        startTime: request.startTime ?? null,
        endTime: request.endTime ?? null,
        campusId: request.campusId ?? null,
        buildingId: request.buildingId ?? null,
        createdAt: request.createdAt ?? null,
        updatedAt: request.updatedAt ?? null,
    };
}

export function useMaintenanceRequestsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.maintenance.requestList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchMaintenanceRequests(requestParams);
            return {
                rows: (response.content ?? []).map(mapRequestToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar solicitudes de mantenimiento') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
