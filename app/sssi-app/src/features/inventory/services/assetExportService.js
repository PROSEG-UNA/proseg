import axios from 'axios';
import { DOCUMENT_PROCESSOR_ENDPOINTS, ASSETS_DOCUMENT_TYPE } from './endpoints.js';

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

export async function exportAssets({ format = XLSX_FORMAT, filename, params = {} } = {}) {
    const url = `${DOCUMENT_PROCESSOR_ENDPOINTS.exports}/${ASSETS_DOCUMENT_TYPE}`;
    const { data, headers } = await axios.get(url, {
        ...config,
        responseType: 'blob',
        params: {
            format,
            ...(filename ? { filename } : {}),
            ...params,
        },
    });

    const fallbackName = `export-assets.${extensionFor(format)}`;
    const resolvedFilename = parseFilenameFromDisposition(headers?.['content-disposition'], fallbackName);

    return {
        blob: data,
        filename: resolvedFilename,
    };
}
