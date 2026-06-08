import axios from 'axios';

const MAINTENANCE_RECORDS_ENDPOINT = '/api/v1/maintenance/records';
const requestConfig = { withCredentials: true };

export async function fetchAssetMaintenanceHistory({ assetId, page = 0, size = 10 } = {}) {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (assetId) params.append('assetId', String(assetId));

    const { data } = await axios.get(MAINTENANCE_RECORDS_ENDPOINT, {
        ...requestConfig,
        params,
    });

    return data?.data ?? { content: [], totalElements: 0 };
}
