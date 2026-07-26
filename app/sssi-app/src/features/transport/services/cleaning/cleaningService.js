import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

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

export async function fetchCleaningHistory(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.cleaningHistory, options);
}

export async function fetchCleaningHistoryById(historyId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.cleaningHistory}/${historyId}`, transportConfig);
    return data?.data ?? null;
}
