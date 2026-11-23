import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

export async function fetchExternalVehicles(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.externalVehicles, options);
}

export async function fetchExternalVehicleById(externalVehicleId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.externalVehicles}/${externalVehicleId}`, transportConfig);
    return data?.data ?? null;
}

export async function createExternalVehicle(payload) {
    const { data } = await axios.post(TRANSPORT_ENDPOINTS.externalVehicles, payload, transportConfig);
    return data?.data;
}

export async function updateExternalVehicle(externalVehicleId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.externalVehicles}/${externalVehicleId}`, payload, transportConfig);
    return data?.data;
}

export async function deleteExternalVehicle(externalVehicleId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.externalVehicles}/${externalVehicleId}`, transportConfig);
    return data?.data;
}
