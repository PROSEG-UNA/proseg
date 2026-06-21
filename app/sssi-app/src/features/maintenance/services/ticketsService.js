// ticketsService.js
import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from './endpoints';
import { fetchPage, maintenanceConfig } from './api';

const TICKETS_BASE = MAINTENANCE_ENDPOINTS.tickets;

function extractPhotoFile(photo) {
    if (!photo) return null;
    if (typeof File !== 'undefined' && photo instanceof File) return photo;
    if (typeof Blob !== 'undefined' && photo instanceof Blob) return photo;
    if (typeof File !== 'undefined' && photo.file instanceof File) return photo.file;
    if (typeof Blob !== 'undefined' && photo.file instanceof Blob) return photo.file;
    return null;
}

function appendPhotos(form, photos = []) {
    photos
        .map(extractPhotoFile)
        .filter(Boolean)
        .forEach((file, index) => {
            const fallbackName = `photo-${index + 1}`;
            form.append('photos', file, file.name ?? fallbackName);
        });
}

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
    appendPhotos(form, photos);

    const { data } = await axios.post(TICKETS_BASE, form, {
        ...maintenanceConfig,
        headers: { 'Content-Type': 'multipart/form-data' },
    });

    return data?.data;
}

export async function updateMaintenanceTicket(ticketId, payload, photos = []) {
    const form = new FormData();
    form.append('ticket', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    appendPhotos(form, photos);

    const { data } = await axios.put(`${TICKETS_BASE}/${ticketId}`, form, {
        ...maintenanceConfig,
        headers: { 'Content-Type': 'multipart/form-data' },
    });

    return data?.data;
}

export async function updateMaintenanceTicketPriority(ticketId, priority) {
    const { data } = await axios.patch(`${TICKETS_BASE}/${ticketId}/priority`, { priority }, maintenanceConfig);
    return data?.data;
}

export async function fetchMaintenanceTicketAssignees() {
    const { data } = await axios.get(`${TICKETS_BASE}/assignees`, maintenanceConfig);
    return Array.isArray(data?.data) ? data.data : [];
}

export async function updateMaintenanceTicketAssignedTo(ticketId, assignedTo) {
    const { data } = await axios.patch(`${TICKETS_BASE}/${ticketId}/assigned-to`, { assignedTo }, maintenanceConfig);
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

export async function updateMaintenanceTicketComment(ticketId, commentId, content) {
    const { data } = await axios.put(
        `${TICKETS_BASE}/${ticketId}/comments/${commentId}`,
        { content },
        maintenanceConfig
    );
    return data?.data;
}

export async function deleteMaintenanceTicketComment(ticketId, commentId) {
    await axios.delete(`${TICKETS_BASE}/${ticketId}/comments/${commentId}`, maintenanceConfig);
}

export async function fetchMaintenanceTicketPhotos(ticketId) {
    const { data } = await axios.get(`${TICKETS_BASE}/${ticketId}/photos`, maintenanceConfig);
    return data?.data ?? [];
}

export async function updateMaintenanceTicketStatus(ticketId, status) {
    const { data } = await axios.patch(`${TICKETS_BASE}/${ticketId}/status`, { status }, maintenanceConfig);
    return data?.data;
}

export async function fetchMaintenanceTicketHistory(ticketId, options = {}) {
    const { page = 0, size = 50 } = options;
    const { data } = await axios.get(`${TICKETS_BASE}/${ticketId}/history`, {
        ...maintenanceConfig,
        params: { page, size },
    });
    return data?.data ?? null;
}