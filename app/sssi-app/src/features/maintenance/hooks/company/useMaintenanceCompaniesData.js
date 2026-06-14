import { useEffect, useState } from 'react';
import { fetchCompanies } from '../../services/company/companiesService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';

export function useMaintenanceCompaniesData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const filtersKey = JSON.stringify(filters);
    const sortKey = sort.join('|');

    useEffect(() => {
        let ignore = false;

        const load = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await fetchCompanies({ page: pageIndex, size: pageSize, search, filters, sort });
                if (ignore) return;

                setRows((response.content ?? []).map((company) => ({
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
                })));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar empresas'));
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        void load();
        return () => {
            ignore = true;
        };
    }, [pageIndex, pageSize, search, filtersKey, sortKey, refreshKey]);

    return { rows, loading, error, totalElements, totalPages };
}
