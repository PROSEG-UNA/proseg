import axios from 'axios';
import {
    DOCUMENT_PROCESSOR_ENDPOINTS,
    MAINTENANCE_REQUESTS_DOCUMENT_TYPE,
    MAINTENANCE_TICKETS_DOCUMENT_TYPE,
} from './endpoints.js';

const config = { withCredentials: true };
const CSV_FORMAT = 'csv';
const XLSX_FORMAT = 'xlsx';

function parseFilenameFromDisposition(contentDisposition, fallback) {
    if (!contentDisposition) return fallback;

    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) {
        try {
            return decodeURIComponent(utf8Match[1].replace(/["']/g, ''));
        } catch {
            return utf8Match[1].replace(/["']/g, '');
        }
    }

    const simpleMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
    if (simpleMatch?.[1]) {
        return simpleMatch[1];
    }

    return fallback;
}

function extensionFor(format) {
    return String(format || '').toLowerCase() === CSV_FORMAT ? CSV_FORMAT : XLSX_FORMAT;
}

async function extractExportErrorMessage(error, fallbackMessage) {
    const responseData = error?.response?.data;

    if (responseData instanceof Blob) {
        try {
            const text = await responseData.text();
            const parsed = JSON.parse(text);
            if (parsed?.message) return parsed.message;
        } catch {
            return fallbackMessage;
        }
    }

    return error?.response?.data?.message || error?.message || fallbackMessage;
}

async function exportMaintenanceDocument({ documentType, format = 'xlsx', filename, params = {} } = {}) {
    const url = `${DOCUMENT_PROCESSOR_ENDPOINTS.exports}/${documentType}`;
    try {
        const { data, headers } = await axios.get(url, {
            ...config,
            responseType: 'blob',
            params: {
                format,
                ...(filename ? { filename } : {}),
                ...params,
            },
        });

        const fallbackName = `export-${documentType}.${extensionFor(format)}`;
        const resolvedFilename = parseFilenameFromDisposition(headers?.['content-disposition'], fallbackName);

        return {
            blob: data,
            filename: resolvedFilename,
        };
    } catch (error) {
        const message = await extractExportErrorMessage(error, 'No fue posible exportar el archivo');
        const parsedError = new Error(message);
        parsedError.cause = error;
        throw parsedError;
    }
}

export function triggerBrowserDownload(blob, filename) {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(objectUrl);
}

export async function exportMaintenanceTickets({ format = 'xlsx', filename, params = {} } = {}) {
    return exportMaintenanceDocument({
        documentType: MAINTENANCE_TICKETS_DOCUMENT_TYPE,
        format,
        filename,
        params,
    });
}

export async function exportMaintenanceRequests({ format = 'xlsx', filename, params = {} } = {}) {
    return exportMaintenanceDocument({
        documentType: MAINTENANCE_REQUESTS_DOCUMENT_TYPE,
        format,
        filename,
        params,
    });
}
