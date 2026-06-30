import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from './endpoints';

const config = { withCredentials: true };

export async function fetchMaintenanceRequests({ page = 0, size = 10, search = '', filters = {}, sort = [] } = {}) {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (search && search.trim() !== '') {
        params.append('search', search.trim());
    }
    Object.entries(filters).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '') return;
        params.append(key, String(value));
    });
    sort.forEach((s) => {
        if (s) params.append('sort', s);
    });

    const { data } = await axios.get(MAINTENANCE_ENDPOINTS.requests, {
        ...config,
        params,
    });

    return data?.data ?? {
        content: [],
        page,
        size,
        totalElements: 0,
        totalPages: 0,
        last: true,
    };
}

export async function fetchMaintenanceRequestById(id) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.requests}/${id}`, config);
    return data?.data ?? null;
}

export async function createMaintenanceRequest(payload) {
    const { data } = await axios.post(MAINTENANCE_ENDPOINTS.requests, payload, config);
    return data?.data;
}

export async function updateMaintenanceRequest(id, payload) {
    const { data } = await axios.put(`${MAINTENANCE_ENDPOINTS.requests}/${id}`, payload, config);
    return data?.data;
}

export async function deleteMaintenanceRequest(id) {
    const { data } = await axios.delete(`${MAINTENANCE_ENDPOINTS.requests}/${id}`, config);
    return data?.data;
}

