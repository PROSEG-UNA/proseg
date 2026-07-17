import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

export async function fetchTours(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.tours, options);
}

export async function fetchTourById(tourId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tours}/${tourId}`, transportConfig);
    return data?.data ?? null;
}

export async function createTour(payload) {
    const { data } = await axios.post(TRANSPORT_ENDPOINTS.tours, payload, transportConfig);
    return data?.data;
}

export async function updateTour(tourId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.tours}/${tourId}`, payload, transportConfig);
    return data?.data;
}

export async function deleteTour(tourId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.tours}/${tourId}`, transportConfig);
    return data?.data;
}
