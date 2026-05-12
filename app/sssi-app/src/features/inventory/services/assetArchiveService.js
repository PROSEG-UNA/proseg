import axios from 'axios';

const BASE_URL = '/api/v1/inventory/assets';
const ARCHIVE_BASE = '/api/v1/archive/files';
const ASSET_ARCHIVES_BASE = '/api/v1/inventory/asset-archives';
const CHUNK_SIZE = 5 * 1024 * 1024;
const config = { withCredentials: true };

export async function fetchAssetArchives(assetId) {
    const { data } = await axios.get(`${BASE_URL}/${assetId}/asset-archives`, config);
    return Array.isArray(data?.data?.content) ? data.data.content : [];
}

export async function uploadPhoto(assetId, file) {
    const initiateParams = new URLSearchParams({
        filename: file.name,
        contentType: file.type,
        folder: `assets/${assetId}/images`,
        totalSize: String(file.size),
    });
    const { data: initData } = await axios.post(
        `${ARCHIVE_BASE}/initiate?${initiateParams}`,
        null,
        config
    );
    const { uploadId, objectName } = initData.data;

    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
    for (let i = 0; i < totalChunks; i++) {
        const chunk = file.slice(i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE);
        const form = new FormData();
        form.append('uploadId', uploadId);
        form.append('objectName', objectName);
        form.append('partNumber', String(i + 1));
        form.append('file', chunk, file.name);
        await axios.post(`${ARCHIVE_BASE}/part`, form, config);
    }

    const completeParams = new URLSearchParams({
        uploadId,
        objectName,
        totalSize: String(file.size),
    });
    await axios.post(`${ARCHIVE_BASE}/complete?${completeParams}`, null, config);

    return objectName;
}

export async function registerArchive(assetId, objectName) {
    const { data } = await axios.post(ASSET_ARCHIVES_BASE, { assetId, objectName }, config);
    return data?.data;
}

export async function deleteArchive(archiveId) {
    await axios.delete(`${ASSET_ARCHIVES_BASE}/${archiveId}`, config);
}
