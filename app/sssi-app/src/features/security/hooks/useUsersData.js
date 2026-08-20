import { useQuery } from '@tanstack/react-query';
import { fetchUsers } from '../services/usersService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function buildFullName(firstName, lastName) {
    return [firstName, lastName].filter(Boolean).join(' ').trim() || '—';
}

function mapStatusToSpanish(status) {
    switch (status) {
        case 'INVITED':
            return 'Invitado';
        case 'APPROVED':
            return 'Activo';
        case 'REJECTED':
            return 'Inactivo';
        case 'PENDING':
        default:
            return 'Pendiente';
    }
}

function mapUserToRow(user) {
    const statusRaw = user.status || 'PENDING';
    return {
        id: user.id,
        username: user.username || '—',
        email: user.email || '—',
        fullName: buildFullName(user.firstName, user.lastName),
        status: mapStatusToSpanish(statusRaw),
        statusRaw,
    };
}

export function useUsersData({ pageIndex = 0, pageSize = 10 } = {}) {
    const requestParams = { page: pageIndex, size: pageSize };

    const listQueryKey = queryKeys.security.userList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchUsers(requestParams);
            return {
                rows: (response.content ?? []).map(mapUserToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar usuarios') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
