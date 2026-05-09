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

export async function fetchAssets({ page = 0, size = 10 } = {}) {
    const { data } = await axios.get(INVENTORY_ENDPOINTS.assets, {
        ...config,
        params: { page, size },
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
