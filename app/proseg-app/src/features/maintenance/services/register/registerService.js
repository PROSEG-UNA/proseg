import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from '../endpoints';
import { fetchPage, maintenanceConfig } from '../api';

export async function fetchAssignedRegisters({ status, ...options } = {}) {
    const filters = status ? { status } : {};
    return fetchPage(`${MAINTENANCE_ENDPOINTS.registers}/assigned`, { ...options, filters });
}

export async function fetchRegistersHistory(options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.registers}/history`, options);
}

export async function fetchRegisterByRequest(requestId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.registers}/by-request/${requestId}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function updateRegister(registerId, payload) {
    const { data } = await axios.put(`${MAINTENANCE_ENDPOINTS.registers}/${registerId}`, payload, maintenanceConfig);
    return data?.data;
}

export async function finalizeRegister(registerId) {
    const { data } = await axios.post(`${MAINTENANCE_ENDPOINTS.registers}/${registerId}/finalize`, null, maintenanceConfig);
    return data?.data;
}

export async function fetchRegisterAssets(registerId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.registers}/${registerId}/assets`, options);
}

export async function createMaintenanceRecord(registerId, payload) {
    const { data } = await axios.post(`${MAINTENANCE_ENDPOINTS.registers}/${registerId}/records`, payload, maintenanceConfig);
    return data?.data;
}

export async function fetchAssetRecords(registerId, { assetId, ...options } = {}) {
    const filters = assetId ? { assetId } : {};
    return fetchPage(`${MAINTENANCE_ENDPOINTS.registers}/${registerId}/records`, { ...options, filters });
}
