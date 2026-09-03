import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

export async function fetchAssignments(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.assignment, options);
}

export async function generateAssignments(payload = {}) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.assignment}/generate`, payload, transportConfig);
    return data?.data;
}

export async function updateAssignment(assignmentId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.assignment}/${assignmentId}`, payload, transportConfig);
    return data?.data;
}
