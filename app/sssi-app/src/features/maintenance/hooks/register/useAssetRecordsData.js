import { useEffect, useState } from 'react';
import { fetchAssetRecords } from '../../services/register/registerService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';

export function useAssetRecordsData({ registerId = null, assetId = null, pageIndex = 0, pageSize = 10, refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);

    useEffect(() => {
        if (!registerId || !assetId) {
            setRows([]);
            setTotalElements(0);
            setLoading(false);
            return undefined;
        }

        let ignore = false;

        const load = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await fetchAssetRecords(registerId, { assetId, page: pageIndex, size: pageSize });
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
                if (!ignore) setError(getFriendlyApiErrorMessage(err, 'Error al cargar el historial del activo'));
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        void load();
        return () => {
            ignore = true;
        };
    }, [registerId, assetId, pageIndex, pageSize, refreshKey]);

    return { rows, loading, error, totalElements };
}
