import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

export async function fetchBorrowedVehicles(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.borrowedVehicles, options);
}

export async function fetchBorrowedVehicleById(borrowedVehicleId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.borrowedVehicles}/${borrowedVehicleId}`, transportConfig);
    return data?.data ?? null;
}

export async function createBorrowedVehicle(payload) {
    const { data } = await axios.post(TRANSPORT_ENDPOINTS.borrowedVehicles, payload, transportConfig);
    return data?.data;
}

export async function updateBorrowedVehicle(borrowedVehicleId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.borrowedVehicles}/${borrowedVehicleId}`, payload, transportConfig);
    return data?.data;
}

export async function deleteBorrowedVehicle(borrowedVehicleId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.borrowedVehicles}/${borrowedVehicleId}`, transportConfig);
    return data?.data;
}
