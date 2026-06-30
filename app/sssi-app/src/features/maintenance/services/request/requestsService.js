import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from '../endpoints';
import { fetchPage, maintenanceConfig } from '../api';

const REQUESTS_BASE = '/api/v1/maintenance/requests';

export async function fetchMaintenanceRequests(options = {}) {
    return fetchPage(REQUESTS_BASE, options);
}

export async function fetchMaintenanceAssets(options = {}) {
    return fetchPage(`${REQUESTS_BASE}/assets`, options);
}

export async function fetchMaintenanceRequestById(requestId) {
    const { data } = await axios.get(`${REQUESTS_BASE}/${requestId}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchMaintenanceRequestsByCompany(companyId, options = {}) {
    return fetchPage(`${REQUESTS_BASE}/company/${companyId}`, options);
}

export async function createMaintenanceRequest(payload) {
    const { data } = await axios.post(REQUESTS_BASE, payload, maintenanceConfig);
    return data?.data;
}

export async function updateMaintenanceRequest(requestId, payload) {
    const { data } = await axios.put(`${REQUESTS_BASE}/${requestId}`, payload, maintenanceConfig);
    return data?.data;
}

export async function acceptMaintenanceRequest(requestId) {
    const { data } = await axios.patch(`${REQUESTS_BASE}/${requestId}/accept`, null, maintenanceConfig);
    return data?.data;
}

export async function cancelMaintenanceRequest(requestId, reason) {
    const { data } = await axios.patch(`${REQUESTS_BASE}/${requestId}/cancel`, { reason }, maintenanceConfig);
    return data?.data;
}

export async function deleteMaintenanceRequest(requestId) {
    const { data } = await axios.delete(`${REQUESTS_BASE}/${requestId}`, maintenanceConfig);
    return data?.data;
}
