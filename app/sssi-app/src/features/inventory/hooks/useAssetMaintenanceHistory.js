import { useEffect, useState } from 'react';
import { fetchAssetMaintenanceHistory } from '../services/maintenanceHistoryService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';

export function useAssetMaintenanceHistory({ assetId = null, pageIndex = 0, pageSize = 10, refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);

    const fetchAndSetHistory = () => {
        if (!assetId) {
            setRows([]);
            setTotalElements(0);
            setLoading(false);
            return undefined;
        }

        let ignore = false;

        const loadHistory = async () => {
            try {
                setLoading(true);
                setError(null);

                const response = await fetchAssetMaintenanceHistory({ assetId, page: pageIndex, size: pageSize });

                if (ignore) return;

                setRows((response.content ?? []).map((record) => ({
                    id: record.id,
                    assetId: record.assetId,
                    companyName: record.company?.name ?? '—',
                    userEmail: record.userEmail ?? '—',
                    description: record.description ?? '—',
                    createdAt: record.createdAt ?? null,
                })));
                setTotalElements(response.totalElements ?? 0);
            } catch (err) {
                if (!ignore) setError(getFriendlyApiErrorMessage(err, 'Error al cargar el historial de mantenimiento'));
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        void loadHistory();

        return () => {
            ignore = true;
        };
    };

    useEffect(fetchAndSetHistory, [assetId, pageIndex, pageSize, refreshKey]);

    return { rows, loading, error, totalElements };
}
