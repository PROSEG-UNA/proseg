import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from './endpoints';
import { fetchPage, maintenanceConfig } from './api';

export async function fetchCampuses(options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.locations}/campuses`, options);
}

export async function fetchBuildingsByCampus(campusId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.locations}/campuses/${campusId}/buildings`, options);
}

export async function fetchCampusById(id) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.locations}/campuses/${id}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchBuildingById(id) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.locations}/buildings/${id}`, maintenanceConfig);
    return data?.data ?? null;
}
