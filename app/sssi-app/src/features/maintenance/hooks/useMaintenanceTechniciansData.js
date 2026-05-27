import { useEffect, useState } from 'react';
import { fetchMaintenanceTechnicians } from '../services/techniciansService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';

export function useMaintenanceTechniciansData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
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
                const response = await fetchMaintenanceTechnicians({ page: pageIndex, size: pageSize, search, filters, sort });
                if (ignore) return;

                setRows((response.content ?? []).map((technician) => ({
                    id: technician.id,
                    fullName: technician.fullName ?? '—',
                    position: technician.position ?? '—',
                    email: technician.email ?? '—',
                    phone: technician.phone ?? '—',
                    leader: !!technician.leader,
                    leaderLabel: technician.leader ? 'Sí' : 'No',
                })));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar técnicos de mantenimiento'));
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

