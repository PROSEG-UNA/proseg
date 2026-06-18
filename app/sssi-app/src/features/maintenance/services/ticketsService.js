// ticketsService.js
import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from './endpoints';
import { fetchPage, maintenanceConfig } from './api';

const TICKETS_BASE = MAINTENANCE_ENDPOINTS.tickets;

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

export async function updateMaintenanceTicket(ticketId, payload, photos = []) {
    const form = new FormData();
    form.append('ticket', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    photos.forEach((photo) => form.append('photos', photo));

    const { data } = await axios.put(`${TICKETS_BASE}/${ticketId}`, form, maintenanceConfig);

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