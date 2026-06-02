import { useEffect, useState } from 'react';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { fetchMaintenanceTickets } from '../services/ticketsService';

export function useMaintenanceTicketsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
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
                const response = await fetchMaintenanceTickets({ page: pageIndex, size: pageSize, search, filters, sort });
                if (ignore) return;

                setRows((response.content ?? []).map((ticket) => ({
                    id: ticket.id,
                    description: ticket.description ?? '—',
                    status: ticket.status ?? '—',
                    priority: ticket.priority ?? '—',
                    statusRaw: ticket.status ?? '',
                    priorityRaw: ticket.priority ?? '',
                    createdBy: ticket.createdBy ?? '—',
                    assignedRole: ticket.assignedRole ?? '',
                    siteId: ticket.siteId ?? null,
                    siteName: ticket.siteName ?? '—',
                    buildingId: ticket.buildingId ?? null,
                    buildingName: ticket.buildingName ?? '—',
                    floorId: ticket.floorId ?? null,
                    floorName: ticket.floorName ?? '—',
                    locationId: ticket.locationId ?? null,
                    locationDescription: ticket.locationDescription ?? '—',
                    assetsCount: ticket.assetsCount ?? 0,
                    photosCount: ticket.photosCount ?? 0,
                    createdAt: ticket.createdAt ?? null,
                    updatedAt: ticket.updatedAt ?? null,
                })));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar tickets de mantenimiento'));
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