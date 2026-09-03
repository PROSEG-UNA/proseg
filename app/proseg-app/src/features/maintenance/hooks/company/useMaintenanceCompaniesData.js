import { useQuery } from '@tanstack/react-query';
import { fetchCompanies } from '../../services/company/companiesService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../../common/query';

function mapCompanyToRow(company) {
    return {
        id: company.id,
        name: company.name ?? '—',
        legalId: company.legalId ?? '—',
        contactEmail: company.contactEmail ?? '—',
        contactPhone: company.contactPhone ?? '—',
        address: company.address ?? '—',
        keycloakUserIds: company.keycloakUserIds ?? [],
        usersCount: Array.isArray(company.keycloakUserIds) ? company.keycloakUserIds.length : 0,
        createdAt: company.createdAt ?? null,
        updatedAt: company.updatedAt ?? null,
    };
}

export function useMaintenanceCompaniesData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.maintenance.companyList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchCompanies(requestParams);
            return {
                rows: (response.content ?? []).map(mapCompanyToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar empresas') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
