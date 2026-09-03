import axios from 'axios';

const BASE_URL = '/api/v1/archive/files';

export async function uploadArchiveFile({
  file,
  folder,
  objectName,
  onProgress,
  chunkSize = 5 * 1024 * 1024,
  retries = 3,
}) {
  if (!file) throw new Error('Archivo requerido');

  const initParams = {
    filename: file.name,
    contentType: file.type || 'application/octet-stream',
  };
  if (folder) initParams.folder = folder;
  if (objectName) initParams.objectName = objectName;

  const initResponse = await axios.post(`${BASE_URL}/initiate`, null, {
    params: initParams,
    withCredentials: true,
  });

  const { uploadId, objectName: resolvedObjectName } = initResponse.data.data;

  const totalSize = file.size;
  const totalChunks = Math.ceil(totalSize / chunkSize);
  let uploadedBytes = 0;

  for (let chunkNumber = 1; chunkNumber <= totalChunks; chunkNumber += 1) {
    const start = (chunkNumber - 1) * chunkSize;
    const end = Math.min(start + chunkSize, totalSize);
    const chunk = file.slice(start, end);

    const formData = new FormData();
    formData.append('file', chunk, file.name);

    let attempt = 0;
    let success = false;

    while (!success && attempt < retries) {
      try {
        await axios.post(`${BASE_URL}/part`, formData, {
          params: {
            uploadId,
            objectName: resolvedObjectName,
            partNumber: chunkNumber,
          },
          withCredentials: true,
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        success = true;
      } catch (err) {
        attempt += 1;
        if (attempt >= retries) {
          throw err;
        }
      }
    }

    uploadedBytes += chunk.size;
    if (onProgress) {
      const percent = Math.round((uploadedBytes * 100) / totalSize);
      onProgress(percent);
    }
  }

  const completeResponse = await axios.post(`${BASE_URL}/complete`, null, {
    params: {
      uploadId,
      objectName: resolvedObjectName,
      totalSize,
    },
    withCredentials: true,
  });

  return completeResponse.data;
}
