import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from './endpoints';
import { fetchPage, maintenanceConfig } from './api';

export async function fetchCampuses(options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.locations}/campuses`, options);
}

export async function fetchBuildingsByCampus(campusId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.locations}/campuses/${campusId}/buildings`, options);
}

export async function fetchFloorsByBuilding(buildingId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.locations}/buildings/${buildingId}/floors`, options);
}

export async function fetchLocationsByBuilding(buildingId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.locations}/buildings/${buildingId}/locations`, options);
}

export async function fetchLocationsByCampus(campusId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.locations}/campuses/${campusId}/locations`, options);
}

export async function fetchAssetsByLocation(locationId, options = {}) {
    return fetchPage(`${MAINTENANCE_ENDPOINTS.locations}/locations/${locationId}/assets`, options);
}

export async function fetchCampusById(id) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.locations}/campuses/${id}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchBuildingById(id) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.locations}/buildings/${id}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchFloorById(id) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.locations}/floors/${id}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchLocationById(id) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.locations}/locations/${id}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function fetchBuildingEmails(buildingId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.locations}/buildings/${buildingId}/emails`, maintenanceConfig);
    return data?.data ?? [];
}

export async function fetchCampusEmails(campusId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.locations}/campuses/${campusId}/emails`, maintenanceConfig);
    return data?.data ?? [];
}