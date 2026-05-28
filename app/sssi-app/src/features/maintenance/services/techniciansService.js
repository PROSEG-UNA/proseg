import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from './endpoints';
import { fetchPage, maintenanceConfig } from './api';

export async function fetchMaintenanceTechnicians(options = {}) {
    return fetchPage(MAINTENANCE_ENDPOINTS.technicians, options);
}

export async function fetchMaintenanceTechnicianById(technicianId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.technicians}/${technicianId}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchTechniciansByRequestId(requestId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.technicians}/maintenance-request/${requestId}`, options);
}

export async function createMaintenanceTechnician(maintenanceRequestId, payload) {
    const url = maintenanceRequestId ? `${MAINTENANCE_ENDPOINTS.technicians}?maintenanceRequestId=${maintenanceRequestId}` : `${MAINTENANCE_ENDPOINTS.technicians}`;
    const { data } = await axios.post(url, payload, maintenanceConfig);
    return data?.data;
}

export async function updateMaintenanceTechnician(technicianId, payload) {
    const { data } = await axios.put(`${MAINTENANCE_ENDPOINTS.technicians}/${technicianId}`, payload, maintenanceConfig);
    return data?.data;
}

export async function deleteMaintenanceTechnician(technicianId) {
    const { data } = await axios.delete(`${MAINTENANCE_ENDPOINTS.technicians}/${technicianId}`, maintenanceConfig);
    return data?.data;
}

