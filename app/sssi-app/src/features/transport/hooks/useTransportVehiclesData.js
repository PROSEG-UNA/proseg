import { useEffect, useMemo, useState } from 'react';
import { fetchVehicles } from '../services/vehicles/vehiclesService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { vehicleStatusLabel } from '../transportUtils';

export function useTransportVehiclesData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
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
                const response = await fetchVehicles({ page: pageIndex, size: pageSize, search, filters: stableFilters, sort: stableSort });
                if (ignore) return;

                setRows((response.content ?? []).map((vehicle) => ({
                    id: vehicle.id,
                    plate: vehicle.plate ?? '—',
                    brand: vehicle.brand ?? '—',
                    model: vehicle.model ?? '—',
                    year: vehicle.year ?? '—',
                    capacity: vehicle.capacity ?? '—',
                    status: vehicleStatusLabel(vehicle.status),
                    statusRaw: vehicle.status ?? '',
                    createdAt: vehicle.createdAt ?? null,
                    updatedAt: vehicle.updatedAt ?? null,
                })));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar vehículos'));
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
