import { useEffect, useState } from 'react';
import { fetchAssignedRegisters, fetchRegistersHistory } from '../../services/register/registerService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { statusLabel } from '../../maintenanceUtils';

function mapRequest(request) {
    return {
        id: request.id,
        companyId: request.company?.id ?? '',
        companyName: request.company?.name ?? '—',
        companyLegalId: request.company?.legalId ?? '—',
        status: statusLabel(request.status),
        statusRaw: request.status ?? '',
        startDate: request.startDate ?? null,
        endDate: request.endDate ?? null,
        startTime: request.startTime ?? null,
        endTime: request.endTime ?? null,
        campusId: request.campusId ?? null,
        buildingId: request.buildingId ?? null,
        leaderName: request.leaderUserCompany?.userEmail ?? request.leaderUserCompany?.keycloakUserId ?? '—',
        techniciansCount: Array.isArray(request.assignedTechnicians) ? request.assignedTechnicians.length : 0,
        createdAt: request.createdAt ?? null,
        updatedAt: request.updatedAt ?? null,
    };
}

export function useMaintenanceRegisterData({ mode = 'assigned', status = null, pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
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
                const response = mode === 'history'
                    ? await fetchRegistersHistory({ page: pageIndex, size: pageSize, search, filters, sort })
                    : await fetchAssignedRegisters({ status, page: pageIndex, size: pageSize, search, filters, sort });
                if (ignore) return;

                setRows((response.content ?? []).map(mapRequest));
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (err) {
                if (!ignore) setError(getFriendlyApiErrorMessage(err, 'Error al cargar registros de mantenimiento'));
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        void load();
        return () => {
            ignore = true;
        };
    }, [mode, status, pageIndex, pageSize, search, filtersKey, sortKey, refreshKey]);

    return { rows, loading, error, totalElements, totalPages };
}
