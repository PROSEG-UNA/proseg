import axios from 'axios';
import { fetchPage, maintenanceConfig } from './api';

const TECHNICIANS_BASE = '/api/v1/maintenance/technicians';

export async function fetchMaintenanceTechnicians(options = {}) {
    return fetchPage(TECHNICIANS_BASE, options);
}

export async function fetchMaintenanceTechnicianById(technicianId) {
    const { data } = await axios.get(`${TECHNICIANS_BASE}/${technicianId}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchTechniciansByRequestId(requestId, options = {}) {
    return fetchPage(`${TECHNICIANS_BASE}/maintenance-request/${requestId}`, options);
}

export async function createMaintenanceTechnician(maintenanceRequestId, payload) {
    const url = maintenanceRequestId ? `${TECHNICIANS_BASE}?maintenanceRequestId=${maintenanceRequestId}` : `${TECHNICIANS_BASE}`;
    const { data } = await axios.post(url, payload, maintenanceConfig);
    return data?.data;
}

export async function updateMaintenanceTechnician(technicianId, payload) {
    const { data } = await axios.put(`${TECHNICIANS_BASE}/${technicianId}`, payload, maintenanceConfig);
    return data?.data;
}

export async function deleteMaintenanceTechnician(technicianId) {
    const { data } = await axios.delete(`${TECHNICIANS_BASE}/${technicianId}`, maintenanceConfig);
    return data?.data;
}

