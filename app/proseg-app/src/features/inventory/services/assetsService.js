import axios from 'axios';
import { INVENTORY_ENDPOINTS } from './endpoints';

const config = { withCredentials: true };

export async function fetchNetworkInterfaceByAsset(assetId) {
    const { data } = await axios.get(`${INVENTORY_ENDPOINTS.networkInterfaces}/asset/${assetId}`, config);
    return data?.data ?? null;
}

export async function createAsset(payload) {
    const { data } = await axios.post(INVENTORY_ENDPOINTS.assets, payload, config);
    return data?.data;
}

export async function updateAsset(id, payload) {
    const { data } = await axios.put(`${INVENTORY_ENDPOINTS.assets}/${id}`, payload, config);
    return data?.data;
}

export async function deleteAsset(id) {
    const { data } = await axios.delete(`${INVENTORY_ENDPOINTS.assets}/${id}`, config);
    return data?.data;
}

export async function fetchAssetById(id) {
    const { data } = await axios.get(`${INVENTORY_ENDPOINTS.assets}/${id}`, config);
    return data?.data ?? null;
}

export async function fetchLastKnownNetworkInterface(assetId) {
    try {
        const { data } = await axios.get(`${INVENTORY_ENDPOINTS.assets}/${assetId}/network-interface/last-known`, config);
        return data?.data ?? null;
    } catch (e) {
        if (e?.response?.status === 404) return null;
        throw e;
    }
}

export async function checkAssetNumber(value, excludeId = null) {
    const params = new URLSearchParams({ value });
    if (excludeId) params.append('excludeId', excludeId);
    const { data } = await axios.get(`${INVENTORY_ENDPOINTS.assets}/check-asset-number?${params}`, config);
    return data?.data ?? false;
}

export async function fetchAssets({ page = 0, size = 10, search = '', filters = {}, sort = [] } = {}) {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (search && search.trim() !== '') {
        params.append('search', search.trim());
    }
    Object.entries(filters).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '') return;
        params.append(key, String(value));
    });
    sort.forEach((s) => {
        if (s) params.append('sort', s);
    });

    const { data } = await axios.get(INVENTORY_ENDPOINTS.assets, {
        ...config,
        params,
    });

    return data?.data ?? {
        content: [],
        page,
        size,
        totalElements: 0,
        totalPages: 0,
        last: true,
    };
}
