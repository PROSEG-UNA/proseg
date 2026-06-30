import axios from 'axios';
import { INVENTORY_ENDPOINTS } from './endpoints';

const config = { withCredentials: true };

export async function fetchAssetComponents(assetId) {
    const { data } = await axios.get(`${INVENTORY_ENDPOINTS.assets}/${assetId}/components`, config);
    return Array.isArray(data?.data) ? data.data : [];
}

export async function createAssetComponent(assetId, payload) {
    const { data } = await axios.post(`${INVENTORY_ENDPOINTS.assets}/${assetId}/components`, payload, config);
    return data?.data;
}

export async function updateAssetComponent(id, payload) {
    const { data } = await axios.put(`${INVENTORY_ENDPOINTS.assetComponents}/${id}`, payload, config);
    return data?.data;
}

export async function deleteAssetComponent(id) {
    const { data } = await axios.delete(`${INVENTORY_ENDPOINTS.assetComponents}/${id}`, config);
    return data?.data;
}

