import axios from 'axios';
import { TRANSPORT_DOCUMENT_ENDPOINTS, TRANSPORT_ENDPOINTS } from '../endpoints';
import { downloadFile, fetchPage, transportConfig } from '../api';

const DRAFT_EXPORT_FALLBACK_FILE_NAME = 'borrador_depurado.xlsx';

export async function importCleaningFile({ file, year, month, businessDays, keepDrivers = true, keepVehicles = true }) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('year', year);
    formData.append('month', month);
    formData.append('businessDays', businessDays);
    formData.append('keepDrivers', keepDrivers);
    formData.append('keepVehicles', keepVehicles);
    const { data } = await axios.post(TRANSPORT_DOCUMENT_ENDPOINTS.cleaningImport, formData, {
        ...transportConfig,
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data?.data ?? null;
}

export async function downloadCleaningTemplate() {
    const { data } = await axios.get(TRANSPORT_DOCUMENT_ENDPOINTS.cleaningImportTemplate, {
        ...transportConfig,
        responseType: 'blob',
    });
    return data;
}

export async function exportCleaningDraft(draftId) {
    return downloadFile(`${TRANSPORT_DOCUMENT_ENDPOINTS.cleaningDraftExport}/${draftId}`, undefined, DRAFT_EXPORT_FALLBACK_FILE_NAME);
}

export async function fetchCleaningDrafts(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.cleaningDrafts, options);
}

export async function fetchCleaningDraft(draftId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}`, transportConfig);
    return data?.data ?? null;
}

export async function fetchCleaningDraftRows(draftId, options = {}) {
    return fetchPage(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/rows`, options);
}

export async function fetchCleaningDraftDriverSummaries(draftId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/drivers/schedule-summary`, transportConfig);
    return data?.data ?? null;
}

export async function fetchCleaningDraftDriverSchedule(draftId, driverId, options = {}) {
    return fetchPage(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/drivers/${driverId}/schedule`, options);
}

export async function createCleaningDraftRow(draftId, payload) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/rows`, payload, transportConfig);
    return data?.data ?? null;
}

export async function updateCleaningDraftRow(draftId, rowId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/rows/${rowId}`, payload, transportConfig);
    return data?.data ?? null;
}

export async function updateCleaningDraftRowDay(draftId, rowId, dayId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/rows/${rowId}/days/${dayId}`, payload, transportConfig);
    return data?.data ?? null;
}

export async function deleteCleaningDraftRow(draftId, rowId) {
    await axios.delete(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/rows/${rowId}`, transportConfig);
}

export async function deleteCleaningDraftDuplicates(draftId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/duplicates`, transportConfig);
    return data?.data ?? 0;
}

export async function deleteCleaningDraft(draftId) {
    await axios.delete(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}`, transportConfig);
}

export async function autoAssignCleaningDraftDrivers(draftId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/drivers/auto-assign`, null, transportConfig);
    return data?.data ?? null;
}

export async function autoAssignCleaningDraftVehicles(draftId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/vehicles/auto-assign`, null, transportConfig);
    return data?.data ?? null;
}

export async function registerCleaningDraft(draftId, replaceExistingInRange) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.cleaningDrafts}/${draftId}/register`, null, {
        ...transportConfig,
        params: { replaceExistingInRange },
    });
    return data?.data ?? null;
}

export async function fetchCleaningHistory(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.cleaningHistory, options);
}

export async function fetchCleaningHistoryById(historyId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.cleaningHistory}/${historyId}`, transportConfig);
    return data?.data ?? null;
}
