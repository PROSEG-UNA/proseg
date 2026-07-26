import { useEffect, useMemo, useState } from 'react';
import { fetchTours } from '../services/tours/toursService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { tourStatusLabel } from '../transportUtils';

export function useTransportToursData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
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
                const response = await fetchTours({ page: pageIndex, size: pageSize, search, filters: stableFilters, sort: stableSort });
                if (ignore) return;

                setRows((response.content ?? []).map((tour) => ({
                    id: tour.id,
                    name: tour.name ?? '—',
                    origin: tour.origin ?? '—',
                    destination: tour.destination ?? '—',
                    startDate: tour.startDate ?? null,
                    endDate: tour.endDate ?? null,
                    driverId: tour.driver?.id ?? tour.driverId ?? '',
                    driverName: [tour.driver?.firstName, tour.driver?.lastName].filter(Boolean).join(' ').trim() || tour.driverName || '—',
                    vehicleId: tour.vehicle?.id ?? tour.vehicleId ?? '',
                    vehiclePlate: tour.vehicle?.plate ?? tour.vehiclePlate ?? '—',
                    status: tourStatusLabel(tour.status),
                    statusRaw: tour.status ?? '',
                    createdAt: tour.createdAt ?? null,
                    updatedAt: tour.updatedAt ?? null,
                })));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar giras'));
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
