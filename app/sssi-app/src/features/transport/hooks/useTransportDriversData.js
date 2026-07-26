import { useEffect, useMemo, useState } from 'react';
import { fetchDrivers } from '../services/drivers/driversService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { driverStatusLabel } from '../transportUtils';

export function useTransportDriversData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const filtersKey = JSON.stringify(filters);
    const sortKey = sort.join('|');
    const stableFilters = useMemo(() => JSON.parse(filtersKey), [filtersKey]);
    const stableSort = useMemo(() => (sortKey ? sortKey.split('|') : []), [sortKey]);

    useEffect(() => {
        let ignore = false;

        const load = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await fetchDrivers({ page: pageIndex, size: pageSize, search, filters: stableFilters, sort: stableSort });
                if (ignore) return;

                setRows((response.content ?? []).map((driver) => ({
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
                })));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar choferes'));
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        void load();
        return () => {
            ignore = true;
        };
    }, [pageIndex, pageSize, search, stableFilters, stableSort, refreshKey]);

    return { rows, loading, error, totalElements, totalPages };
}
