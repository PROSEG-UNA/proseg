import axios from 'axios';
import { TRANSPORT_DOCUMENT_ENDPOINTS, TRANSPORT_ENDPOINTS } from '../endpoints';
import { downloadFile, fetchPage, transportConfig } from '../api';

const DRAFT_EXPORT_FALLBACK_FILE_NAME = 'borrador_depurado.xlsx';
const FINAL_COMMISSION_FALLBACK_FILE_NAME = 'comision_final.xlsx';
const TOUR_CONTROL_FALLBACK_FILE_NAME = 'control_de_giras.xlsx';

export async function importTourProcessingFile({ file, year, month, businessDays, keepDrivers = true, keepVehicles = true }) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('year', year);
    formData.append('month', month);
    formData.append('businessDays', businessDays);
    formData.append('keepDrivers', keepDrivers);
    formData.append('keepVehicles', keepVehicles);
    const { data } = await axios.post(TRANSPORT_DOCUMENT_ENDPOINTS.tourProcessingImport, formData, {
        ...transportConfig,
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data?.data ?? null;
}

export async function downloadTourProcessingTemplate() {
    const { data } = await axios.get(TRANSPORT_DOCUMENT_ENDPOINTS.tourProcessingImportTemplate, {
        ...transportConfig,
        responseType: 'blob',
    });
    return data;
}

export async function exportTourProcessingDraft(draftId) {
    return downloadFile(`${TRANSPORT_DOCUMENT_ENDPOINTS.tourProcessingDraftExport}/${draftId}`, undefined, DRAFT_EXPORT_FALLBACK_FILE_NAME);
}

export async function exportTourProcessingDraftTourControl(draftId) {
    return downloadFile(`${TRANSPORT_DOCUMENT_ENDPOINTS.tourProcessingDraftExport}/${draftId}/tour-control`, undefined, TOUR_CONTROL_FALLBACK_FILE_NAME);
}

export async function exportMonthTourControl(year, month) {
    return downloadFile(`${TRANSPORT_DOCUMENT_ENDPOINTS.tourProcessingMonthExport}/${year}/${month}/tour-control`, undefined, TOUR_CONTROL_FALLBACK_FILE_NAME);
}

export async function checkTourProcessingDraftFinalCommission(draftId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/final-commission/check`, transportConfig);
    return data?.data ?? null;
}

export async function rejectTourProcessingDraftIncompleteTours(draftId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/final-commission/reject-incomplete`, null, transportConfig);
    return data?.data ?? 0;
}

export async function exportTourProcessingDraftFinalCommission(draftId) {
    return downloadFile(`${TRANSPORT_DOCUMENT_ENDPOINTS.tourProcessingDraftExport}/${draftId}/final-commission`, undefined, FINAL_COMMISSION_FALLBACK_FILE_NAME);
}

export async function fetchTourProcessingDrafts(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.tourProcessingDrafts, options);
}

export async function fetchTourProcessingDraft(draftId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}`, transportConfig);
    return data?.data ?? null;
}

export async function finishTourProcessingDraft(draftId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/finish`, null, transportConfig);
    return data?.data ?? null;
}

export async function reopenTourProcessingDraft(draftId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/reopen`, null, transportConfig);
    return data?.data ?? null;
}

export async function fetchTourProcessingDraftRows(draftId, options = {}) {
    return fetchPage(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows`, options);
}

export async function fetchTourProcessingDraftRow(draftId, rowId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}`, transportConfig);
    return data?.data ?? null;
}

export async function fetchTourProcessingDraftDriverSummaries(draftId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/drivers/schedule-summary`, transportConfig);
    return data?.data ?? null;
}

export async function fetchTourProcessingDraftSchedule(draftId, date) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/drivers/schedule`, { ...transportConfig, params: { date } });
    return data?.data ?? null;
}

export async function fetchTourProcessingDraftDriverSchedule(draftId, driverId, options = {}) {
    return fetchPage(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/drivers/${driverId}/schedule`, options);
}

export async function createTourProcessingDraftRow(draftId, payload) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows`, payload, transportConfig);
    return data?.data ?? null;
}

export async function updateTourProcessingDraftRow(draftId, rowId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}`, payload, transportConfig);
    return data?.data ?? null;
}

export async function previewNewTourProcessingDraftRowWarnings(draftId, payload) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/warnings`, payload, transportConfig);
    return data?.data ?? [];
}

export async function previewTourProcessingDraftRowWarnings(draftId, rowId, payload) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}/warnings`, payload, transportConfig);
    return data?.data ?? [];
}

export async function previewTourProcessingDraftApprovalWarnings(draftId, rowId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}/approval-warnings`, null, transportConfig);
    return data?.data ?? [];
}

export async function updateTourObservationReviewed(draftId, rowId, reviewed) {
    const { data } = await axios.patch(
        `${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}/observation-reviewed`,
        { reviewed },
        transportConfig,
    );
    return data?.data ?? null;
}

export async function updateTourProcessingDraftRowApprovalStatus(draftId, rowId, approvalStatus) {
    const { data } = await axios.patch(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}/approval-status`, { approvalStatus }, transportConfig);
    return data?.data ?? null;
}

export async function updateTourProcessingDraftRowDay(draftId, rowId, dayId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}/days/${dayId}`, payload, transportConfig);
    return data?.data ?? null;
}

export async function previewTourProcessingDraftRowDayWarnings(draftId, rowId, dayId, payload) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}/days/${dayId}/warnings`, payload, transportConfig);
    return data?.data ?? [];
}

export async function deleteTourProcessingDraftRow(draftId, rowId) {
    await axios.delete(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/rows/${rowId}`, transportConfig);
}

export async function deleteTourProcessingDraftDuplicates(draftId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/duplicates`, transportConfig);
    return data?.data ?? 0;
}

export async function splitTourProcessingDraftOversizedRows(draftId, mode = 'DISTINCT_VEHICLES') {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/split-oversized`, null, { ...transportConfig, params: { mode } });
    return data?.data ?? 0;
}

export async function deleteTourProcessingDraft(draftId) {
    await axios.delete(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}`, transportConfig);
}

export async function autoAssignTourProcessingDraftDrivers(draftId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/drivers/auto-assign`, null, transportConfig);
    return data?.data ?? null;
}

export async function autoAssignTourProcessingDraftUlloaDrivers(draftId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/drivers/auto-assign-ulloa`, null, transportConfig);
    return data?.data ?? null;
}

export async function autoAssignTourProcessingDraftVehicles(draftId) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/vehicles/auto-assign`, null, transportConfig);
    return data?.data ?? null;
}

export async function clearTourProcessingDraftAssignments(draftId) {
    await axios.post(`${TRANSPORT_ENDPOINTS.tourProcessingDrafts}/${draftId}/assignments/clear`, null, transportConfig);
}
