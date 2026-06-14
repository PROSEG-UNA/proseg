import { useEffect, useState } from 'react';
import { fetchRegisterAssets } from '../../services/register/registerService';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';

function mapStatusToSpanish(status) {
    switch (status) {
        case 'APROBADO': return 'Aprobado';
        case 'DE_BAJA': return 'De baja';
        default: return status || '—';
    }
}

function mapAssetRow(asset) {
    const statusRaw = asset.status || '';
    return {
        id: asset.id,
        kind: asset.kind || '—',
        assetNumber: asset.assetNumber ?? '—',
        serialNumber: asset.serialNumber ?? '—',
        type: asset.type || '—',
        brand: asset.brand || '—',
        model: asset.modelName || '—',
        campus: asset.campusName || '—',
        building: asset.buildingName || '—',
        floor: asset.floorName || '—',
        location: asset.locationName || '—',
        executingUnit: asset.executingUnit || '—',
        responsibleEmployee: asset.responsibleEmployee || '—',
        responsibleEmployeeId: asset.responsibleEmployeeId || '—',
        status: mapStatusToSpanish(statusRaw),
        statusRaw,
        acquisitionDate: asset.acquisitionDate || null,
        warrantyEndDate: asset.warrantyEndDate || null,
        firmwareSupportEndDate: asset.firmwareSupportEndDate || null,
        decommissionDate: asset.decommissionDate || null,
        latitude: asset.latitude ?? null,
        longitude: asset.longitude ?? null,
    };
}

export function useRegisterAssetsData({ registerId = null, pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);

    const filtersKey = JSON.stringify(filters);
    const sortKey = sort.join('|');

    const fetchAndSetAssets = () => {
        if (!registerId) {
            setRows([]);
            setTotalElements(0);
            setLoading(false);
            return undefined;
        }

        let ignore = false;

        const loadAssets = async () => {
            try {
                setLoading(true);
                setError(null);

                const response = await fetchRegisterAssets(registerId, { page: pageIndex, size: pageSize, search, filters, sort });

                if (ignore) return;

                setRows((response.content ?? []).map(mapAssetRow));
                setTotalElements(response.totalElements ?? 0);
            } catch (err) {
                if (!ignore) {
                    setError(getFriendlyApiErrorMessage(err, 'Error al cargar los activos'));
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

    useEffect(fetchAndSetAssets, [registerId, pageIndex, pageSize, search, filtersKey, sortKey, refreshKey]);

    return { rows, loading, error, totalElements };
}
