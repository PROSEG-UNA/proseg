import { downloadFile } from './api.js';
import { TRANSPORT_DOCUMENT_ENDPOINTS } from './endpoints.js';

export function exportVehicles(params = {}) {
    return downloadFile(`${TRANSPORT_DOCUMENT_ENDPOINTS.exports}/vehicles`, { format: 'xlsx', ...params }, 'vehiculos.xlsx');
}

export function exportBorrowedVehicles(params = {}) {
    return downloadFile(`${TRANSPORT_DOCUMENT_ENDPOINTS.exports}/borrowed-vehicles`, { format: 'xlsx', ...params }, 'vehiculos-prestados.xlsx');
}

export function exportDrivers(params = {}) {
    return downloadFile(`${TRANSPORT_DOCUMENT_ENDPOINTS.exports}/drivers`, { format: 'xlsx', ...params }, 'choferes.xlsx');
}
