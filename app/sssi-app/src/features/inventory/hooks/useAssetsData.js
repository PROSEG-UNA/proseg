import { useState, useEffect } from 'react';
import { fetchAssets } from '../services/assetsService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';

function mapStatusToSpanish(status) {
    switch (status) {
        case 'BUENO': return 'Bueno';
        case 'REGULAR': return 'Regular';
        case 'MALO': return 'Malo';
        case 'EN_REPARACION': return 'En reparación';
        case 'BAJA': return 'Baja';
        default: return status || '—';
    }
}

export function useAssetsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const filtersKey = JSON.stringify(filters);
    const sortKey = sort.join('|');

    const fetchAndSetAssets = () => {
        let ignore = false;

        const loadAssets = async () => {
            try {
                setLoading(true);
                setError(null);

                const response = await fetchAssets({ page: pageIndex, size: pageSize, search, filters, sort });

                if (ignore) return;

                const mappedRows = (response.content ?? []).map((asset) => {
                    const statusRaw = asset.status || '';
                    return {
                        id: asset.id,
                        kind: asset.kind || '—',
                        name: asset.name || '—',
                        description: asset.description || '—',
                        brand: asset.model?.brand?.name || '—',
                        model: asset.model?.name || '—',
                        type: asset.model?.type?.name || '—',
                        location: asset.location?.name || '—',
                        site: asset.location?.site?.name || '—',
                        status: mapStatusToSpanish(statusRaw),
                        statusRaw,
                        statusDescription: asset.statusDescription || '—',
                        acquisitionDate: asset.acquisitionDate || null,
                        warrantyEndDate: asset.warrantyEndDate || null,
                        firmwareSupportEndDate: asset.firmwareSupportEndDate || null,
                    };
                });

                setRows(mappedRows);
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (err) {
                if (!ignore) {
                    setError(getFriendlyApiErrorMessage(err, 'Error al cargar activos'));
                }
            } finally {
                if (!ignore) {
                    setLoading(false);
                }
            }
        };

        void loadAssets();

        return () => {
            ignore = true;
        };
    };

    useEffect(fetchAndSetAssets, [pageIndex, pageSize, search, filtersKey, sortKey, refreshKey]);

    return { rows, loading, error, totalElements, totalPages };
}
