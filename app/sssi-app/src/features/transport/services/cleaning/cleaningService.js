import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { transportConfig } from '../api';

export async function previewCleaning(file) {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.cleaning}/preview`, formData, {
        ...transportConfig,
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data?.data ?? null;
}

export async function finalizeCleaning(payload) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.cleaning}/finalize`, payload, transportConfig);
    return data?.data ?? null;
}

export async function registerCleaningRows(payload) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.cleaning}/register`, payload, transportConfig);
    return data?.data ?? null;
}
