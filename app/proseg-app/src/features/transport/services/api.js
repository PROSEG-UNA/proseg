import axios from 'axios';

export const transportConfig = { withCredentials: true };

function fileNameFromDisposition(contentDisposition, fallbackFileName) {
    const encodedName = contentDisposition?.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    if (encodedName) return decodeURIComponent(encodedName);
    return contentDisposition?.match(/filename="?([^";]+)"?/i)?.[1] ?? fallbackFileName;
}

async function readBlobError(error) {
    const body = error?.response?.data;
    if (!(body instanceof Blob)) return;
    try {
        error.response.data = JSON.parse(await body.text());
    } catch {
        error.response.data = null;
    }
}

export async function downloadFile(url, params, fallbackFileName) {
    try {
        const { data, headers } = await axios.get(url, {
            ...transportConfig,
            params,
            responseType: 'blob',
        });
        return { blob: data, fileName: fileNameFromDisposition(headers?.['content-disposition'], fallbackFileName) };
    } catch (error) {
        await readBlobError(error);
        throw error;
    }
}

export function buildPageParams({ page = 0, size = 10, search = '', filters = {}, sort = [], rule = '' } = {}) {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (search && search.trim() !== '') {
        params.append('search', search.trim());
    }
    if (rule) {
        params.append('rule', rule);
    }

    Object.entries(filters).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '') return;
        params.append(key, String(value));
    });

    sort.forEach((value) => {
        if (value) params.append('sort', value);
    });

    return params;
}

export function emptyPage(page = 0, size = 10) {
    return {
        content: [],
        page,
        size,
        totalElements: 0,
        totalPages: 0,
        last: true,
    };
}

export async function fetchPage(baseUrl, options = {}) {
    const params = buildPageParams(options);
    const { data } = await axios.get(baseUrl, {
        ...transportConfig,
        params,
    });

    return data?.data ?? emptyPage(options.page, options.size);
}
