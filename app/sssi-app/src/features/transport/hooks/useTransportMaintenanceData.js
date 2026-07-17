import { useEffect, useMemo, useState } from 'react';
import { fetchVehicleMaintenance } from '../services/maintenance/maintenanceService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { maintenanceStatusLabel } from '../transportUtils';

export function useTransportMaintenanceData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
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
                const response = await fetchVehicleMaintenance({ page: pageIndex, size: pageSize, search, filters: stableFilters, sort: stableSort });
                if (ignore) return;

                setRows((response.content ?? []).map((record) => ({
                    id: record.id,
                    vehicleId: record.vehicle?.id ?? record.vehicleId ?? '',
                    vehiclePlate: record.vehicle?.plate ?? record.vehiclePlate ?? '—',
                    title: record.title ?? record.description ?? '—',
                    type: record.type ?? '—',
                    scheduledDate: record.scheduledDate ?? null,
                    cost: record.cost ?? null,
                    status: maintenanceStatusLabel(record.status),
                    statusRaw: record.status ?? '',
                    createdAt: record.createdAt ?? null,
                    updatedAt: record.updatedAt ?? null,
                    notes: record.notes ?? '',
                })));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar mantenimientos'));
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
