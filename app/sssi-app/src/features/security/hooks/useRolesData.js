import { useQuery } from '@tanstack/react-query';
import { fetchRoles } from '../services/rolesService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

export function useRolesData({ pageIndex = 0, pageSize = 10 } = {}) {
    const requestParams = { page: pageIndex, size: pageSize };

    const listQueryKey = queryKeys.security.roleList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchRoles(requestParams);

            const rows = (response.content ?? []).map((role) => ({
                id: role.id,
                name: role.name,
                description: role.description || '—',
            }));

            return { rows, totalElements: response.totalElements ?? 0 };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar roles') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
