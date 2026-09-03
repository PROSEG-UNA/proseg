import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

export async function fetchVehicles(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.vehicles, options);
}

export async function fetchVehicleById(vehicleId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.vehicles}/${vehicleId}`, transportConfig);
    return data?.data ?? null;
}

export async function createVehicle(payload) {
    const { data } = await axios.post(TRANSPORT_ENDPOINTS.vehicles, payload, transportConfig);
    return data?.data;
}

export async function updateVehicle(vehicleId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.vehicles}/${vehicleId}`, payload, transportConfig);
    return data?.data;
}

export async function deleteVehicle(vehicleId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.vehicles}/${vehicleId}`, transportConfig);
    return data?.data;
}
