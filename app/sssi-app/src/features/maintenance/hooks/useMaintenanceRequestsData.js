import { useState, useEffect } from 'react';
import { fetchMaintenanceRequests } from '../services/maintenanceService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';

function mapStatusToSpanish(status) {
    switch (status) {
        case 'PENDING': return 'Pendiente';
        case 'IN_PROGRESS': return 'En progreso';
        case 'COMPLETED': return 'Completado';
        case 'CANCELLED': return 'Cancelado';
        default: return status || '—';
    }
}

function mapPriorityToSpanish(priority) {
    switch (priority) {
        case 'LOW': return 'Baja';
        case 'MEDIUM': return 'Media';
        case 'HIGH': return 'Alta';
        case 'URGENT': return 'Urgente';
        default: return priority || '—';
    }
}

export function useMaintenanceRequestsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const filtersKey = JSON.stringify(filters);
    const sortKey = sort.join('|');

    const fetchAndSetRequests = () => {
        let ignore = false;

        const loadRequests = async () => {
            try {
                setLoading(true);
                setError(null);

                const response = await fetchMaintenanceRequests({ page: pageIndex, size: pageSize, search, filters, sort });

                if (ignore) return;

                const mappedRows = (response.content ?? []).map((request) => {
                    const statusRaw = request.status || '';
                    const priorityRaw = request.priority || '';
                    return {
                        id: request.id,
                        title: request.title || '—',
                        description: request.description || '—',
                        assetId: request.assetId ?? '—',
                        company: request.company?.name || '—',
                        status: mapStatusToSpanish(statusRaw),
                        statusRaw,
                        priority: mapPriorityToSpanish(priorityRaw),
                        priorityRaw,
                        scheduledDate: request.scheduledDate || null,
                        observations: request.observations || '—',
                        technicians: request.technicians || [],
                        createdAt: request.createdAt || null,
                        updatedAt: request.updatedAt || null,
                    };
                });

                setRows(mappedRows);
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (err) {
                if (!ignore) {
                    setError(getFriendlyApiErrorMessage(err, 'Error al cargar solicitudes'));
                }
            } finally {
                if (!ignore) {
                    setLoading(false);
                }
            }
        };

        void loadRequests();

        return () => {
            ignore = true;
        };
    };

    useEffect(fetchAndSetRequests, [pageIndex, pageSize, search, filtersKey, sortKey, refreshKey]);

    return { rows, loading, error, totalElements, totalPages };
}

