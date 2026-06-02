import axios from 'axios';
import { fetchCatalogOptions, fetchCatalogPage } from '../../inventory/services/catalogService';
import { fetchPage, maintenanceConfig } from './api';

const TICKETS_BASE = '/api/v1/maintenance/tickets';
const CAMPUSES_BASE = '/api/v1/inventory/campuses';
const BUILDINGS_BASE = '/api/v1/inventory/buildings';
const FLOORS_BASE = '/api/v1/inventory/floors';
const LOCATIONS_BASE = '/api/v1/inventory/locations';
const ASSETS_BASE = '/api/v1/inventory/assets';

const CATALOG_PAGE_SIZE = 200;

export async function fetchMaintenanceTickets(options = {}) {
    return fetchPage(TICKETS_BASE, options);
}

export async function fetchMaintenanceTicketById(ticketId) {
    const { data } = await axios.get(`${TICKETS_BASE}/${ticketId}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function createMaintenanceTicket(payload, photos = []) {
    const form = new FormData();
    form.append('ticket', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    photos.forEach((photo) => form.append('photos', photo));

    const { data } = await axios.post(TICKETS_BASE, form, maintenanceConfig);

    return data?.data;
}

export async function updateMaintenanceTicketPriority(ticketId, priority) {
    const { data } = await axios.patch(`${TICKETS_BASE}/${ticketId}/priority`, { priority }, maintenanceConfig);
    return data?.data;
}

export async function updateMaintenanceTicketAssignedRole(ticketId, assignedRole) {
    const { data } = await axios.patch(`${TICKETS_BASE}/${ticketId}/assigned-role`, { assignedRole }, maintenanceConfig);
    return data?.data;
}

export async function resolveMaintenanceTicket(ticketId) {
    const { data } = await axios.patch(`${TICKETS_BASE}/${ticketId}/resolve`, null, maintenanceConfig);
    return data?.data;
}

export async function addMaintenanceTicketComment(ticketId, content) {
    const { data } = await axios.post(`${TICKETS_BASE}/${ticketId}/comments`, { content }, maintenanceConfig);
    return data?.data;
}

export async function fetchTicketCampuses() {
    return fetchCatalogOptions(CAMPUSES_BASE);
}

export async function fetchTicketBuildings(campusId = null) {
    if (!campusId) return fetchCatalogOptions(BUILDINGS_BASE);
    const page = await fetchCatalogPage(`${BUILDINGS_BASE}/campus/${campusId}`, { page: 0, size: CATALOG_PAGE_SIZE });
    return page.content ?? [];
}

export async function fetchTicketFloors(buildingId = null) {
    if (!buildingId) return fetchCatalogOptions(FLOORS_BASE);
    const page = await fetchCatalogPage(`${FLOORS_BASE}/building/${buildingId}`, { page: 0, size: CATALOG_PAGE_SIZE });
    return page.content ?? [];
}

export async function fetchTicketLocations(campusId = null, buildingId = null) {
    if (buildingId) {
        const page = await fetchCatalogPage(`${LOCATIONS_BASE}/building/${buildingId}`, { page: 0, size: CATALOG_PAGE_SIZE });
        return page.content ?? [];
    }

    if (campusId) {
        const page = await fetchCatalogPage(`${LOCATIONS_BASE}/campus/${campusId}`, { page: 0, size: CATALOG_PAGE_SIZE });
        return page.content ?? [];
    }

    return fetchCatalogOptions(LOCATIONS_BASE);
}

export async function fetchTicketAssets({ page = 0, size = 200, search = '' } = {}) {
    const pageData = await fetchCatalogPage(ASSETS_BASE, { page, size, search });
    return pageData.content ?? [];
}

export async function updateMaintenanceTicket(ticketId, payload, photos = []) {
    const form = new FormData();
    form.append('ticket', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    photos.forEach((photo) => form.append('photos', photo));

    const { data } = await axios.put(`${TICKETS_BASE}/${ticketId}`, form, maintenanceConfig);

    return data?.data;
}