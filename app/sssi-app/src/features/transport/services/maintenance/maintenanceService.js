import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

export async function fetchVehicleMaintenance(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.maintenance, options);
}

export async function fetchVehicleMaintenanceById(maintenanceId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.maintenance}/${maintenanceId}`, transportConfig);
    return data?.data ?? null;
}

export async function createVehicleMaintenance(payload) {
    const { data } = await axios.post(TRANSPORT_ENDPOINTS.maintenance, payload, transportConfig);
    return data?.data;
}

export async function updateVehicleMaintenance(maintenanceId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.maintenance}/${maintenanceId}`, payload, transportConfig);
    return data?.data;
}

export async function deleteVehicleMaintenance(maintenanceId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.maintenance}/${maintenanceId}`, transportConfig);
    return data?.data;
}
