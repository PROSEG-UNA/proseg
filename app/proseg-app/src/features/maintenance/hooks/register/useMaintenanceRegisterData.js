import { useQuery } from '@tanstack/react-query';
import { fetchAssignedRegisters, fetchRegistersHistory } from '../../services/register/registerService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { statusLabel } from '../../maintenanceUtils';
import { keepPreviousPage, queryKeys } from '../../../../common/query';

function mapRequest(request) {
    return {
        id: request.id,
        companyId: request.company?.id ?? '',
        companyName: request.company?.name ?? '—',
        companyLegalId: request.company?.legalId ?? '—',
        status: statusLabel(request.status),
        statusRaw: request.status ?? '',
        startDate: request.startDate ?? null,
        endDate: request.endDate ?? null,
        startTime: request.startTime ?? null,
        endTime: request.endTime ?? null,
        campusId: request.campusId ?? null,
        buildingId: request.buildingId ?? null,
        responsibleName: request.responsibleUserCompany?.userEmail ?? request.responsibleUserCompany?.keycloakUserId ?? '—',
        techniciansCount: Array.isArray(request.assignedTechnicians) ? request.assignedTechnicians.length : 0,
        createdAt: request.createdAt ?? null,
        updatedAt: request.updatedAt ?? null,
    };
}

export function useMaintenanceRegisterData({ mode = 'assigned', status = null, pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.maintenance.registerList({ mode, status }, requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = mode === 'history'
                ? await fetchRegistersHistory(requestParams)
                : await fetchAssignedRegisters({ status, ...requestParams });

            return {
                rows: (response.content ?? []).map(mapRequest),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar registros de mantenimiento') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
