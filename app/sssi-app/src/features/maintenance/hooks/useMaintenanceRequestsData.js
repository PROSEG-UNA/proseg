import { useEffect, useState } from 'react';
import { fetchMaintenanceRequests } from '../services/requestsService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { formatDate, priorityLabel, statusLabel } from '../maintenanceUtils';

export function useMaintenanceRequestsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
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
                const response = await fetchMaintenanceRequests({ page: pageIndex, size: pageSize, search, filters, sort });
                if (ignore) return;

                setRows((response.content ?? []).map((request) => {
                    const technicians = Array.isArray(request.technicians) ? request.technicians : [];
                    return {
                        id: request.id,
                        companyId: request.company?.id ?? '',
                        companyName: request.company?.name ?? '—',
                        companyLegalId: request.company?.legalId ?? '—',
                        assetId: request.assetId ?? '—',
                        title: request.title ?? '—',
                        description: request.description ?? '—',
                        status: statusLabel(request.status),
                        statusRaw: request.status ?? '',
                        priority: priorityLabel(request.priority),
                        priorityRaw: request.priority ?? '',
                        scheduledDate: request.scheduledDate ?? null,
                        scheduledDateLabel: formatDate(request.scheduledDate),
                        observations: request.observations ?? '—',
                        techniciansCount: technicians.length,
                        technicians,
                        createdAt: request.createdAt ?? null,
                        updatedAt: request.updatedAt ?? null,
                    };
                }));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar solicitudes de mantenimiento'));
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

