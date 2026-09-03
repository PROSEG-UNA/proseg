import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

export async function fetchDrivers(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.drivers, options);
}

export async function fetchDriverById(driverId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.drivers}/${driverId}`, transportConfig);
    return data?.data ?? null;
}

export async function createDriver(payload) {
    const { data } = await axios.post(TRANSPORT_ENDPOINTS.drivers, payload, transportConfig);
    return data?.data;
}

export async function updateDriver(driverId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.drivers}/${driverId}`, payload, transportConfig);
    return data?.data;
}

export async function deleteDriver(driverId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.drivers}/${driverId}`, transportConfig);
    return data?.data;
}
