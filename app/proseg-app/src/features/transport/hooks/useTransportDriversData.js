import { useQuery } from '@tanstack/react-query';
import { fetchDrivers } from '../services/drivers/driversService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { driverStatusLabel } from '../transportUtils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapDriverToRow(driver) {
    return {
        id: driver.id,
        firstName: driver.firstName ?? '',
        lastName: driver.lastName ?? '',
        fullName: [driver.firstName, driver.lastName].filter(Boolean).join(' ').trim() || driver.name || '—',
        documentId: driver.documentId ?? '—',
        licenseNumber: driver.licenseNumber ?? '—',
        phone: driver.phone ?? '—',
        email: driver.email ?? '—',
        status: driverStatusLabel(driver.status),
        statusRaw: driver.status ?? '',
        createdAt: driver.createdAt ?? null,
        updatedAt: driver.updatedAt ?? null,
    };
}

export function useTransportDriversData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.transport.driverList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchDrivers(requestParams);
            return {
                rows: (response.content ?? []).map(mapDriverToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar choferes') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
