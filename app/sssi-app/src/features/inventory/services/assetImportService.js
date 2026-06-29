import axios from 'axios';
import { DOCUMENT_PROCESSOR_ENDPOINTS, ASSETS_DOCUMENT_TYPE } from './endpoints.js';

const config = { withCredentials: true };

export async function previewAssetsExcel(file) {
    const formData = new FormData();
    formData.append('file', file);
    const url = `${DOCUMENT_PROCESSOR_ENDPOINTS.imports}/${ASSETS_DOCUMENT_TYPE}/preview`;
    const { data } = await axios.post(url, formData, {
        ...config,
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data?.data;
}

export async function confirmAssetsImport(rows, approvedKeys) {
    const url = `${DOCUMENT_PROCESSOR_ENDPOINTS.imports}/${ASSETS_DOCUMENT_TYPE}/confirm`;
    const { data } = await axios.post(url, { rows, approvedKeys }, config);
    return data?.data;
}

export async function downloadAssetsTemplate() {
    const url = `${DOCUMENT_PROCESSOR_ENDPOINTS.imports}/${ASSETS_DOCUMENT_TYPE}/template`;
    const { data } = await axios.get(url, { ...config, responseType: 'blob' });
    return data;
}
