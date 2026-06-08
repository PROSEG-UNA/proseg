import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from '../endpoints';
import { fetchPage, maintenanceConfig } from '../api';

export async function fetchMaintenanceRequests(options = {}) {
    return fetchPage(MAINTENANCE_ENDPOINTS.requests, options);
}

export async function fetchMaintenanceAssets(options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.requests}/assets`, options);
}

export async function fetchMaintenanceRequestById(requestId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.requests}/${requestId}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchMaintenanceRequestsByCompany(companyId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.requests}/company/${companyId}`, options);
}

export async function createMaintenanceRequest(payload) {
    const { data } = await axios.post(MAINTENANCE_ENDPOINTS.requests, payload, maintenanceConfig);
    return data?.data;
}

export async function updateMaintenanceRequest(requestId, payload) {
    const { data } = await axios.put(`${MAINTENANCE_ENDPOINTS.requests}/${requestId}`, payload, maintenanceConfig);
    return data?.data;
}

export async function deleteMaintenanceRequest(requestId) {
    const { data } = await axios.delete(`${MAINTENANCE_ENDPOINTS.requests}/${requestId}`, maintenanceConfig);
    return data?.data;
}
