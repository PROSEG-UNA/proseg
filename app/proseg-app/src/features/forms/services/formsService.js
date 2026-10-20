import axios from 'axios';
import { FORMS_ENDPOINTS } from './endpoints';
import { fetchPage, maintenanceConfig } from '../../maintenance/services/api';

export async function fetchFormTypes() {
    const { data } = await axios.get(FORMS_ENDPOINTS.types, maintenanceConfig);
    return data?.data ?? [];
}

export async function fetchFormRecords(options = {}) {
    return fetchPage(FORMS_ENDPOINTS.records, options);
}

export async function createFormRecord(payload) {
    const { data } = await axios.post(FORMS_ENDPOINTS.records, payload, maintenanceConfig);
    return data?.data;
}
