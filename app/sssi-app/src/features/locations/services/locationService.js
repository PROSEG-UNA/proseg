import axios from 'axios';

const config = { withCredentials: true };

export async function fetchLocationPage(baseUrl, { page = 0, size = 10, search = '', filters = {}, sort = [] } = {}) {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (search && search.trim() !== '') params.append('search', search.trim());
    Object.entries(filters).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '') return;
        params.append(key, String(value));
    });
    sort.forEach((s) => { if (s) params.append('sort', s); });

    const { data } = await axios.get(baseUrl, { ...config, params });
    return data?.data ?? { content: [], page, size, totalElements: 0, totalPages: 0, last: true };
}

export async function fetchLocationOptions(baseUrl) {
    const { data } = await axios.get(baseUrl, {
        ...config,
        params: { page: 0, size: 200 },
    });
    return data?.data?.content ?? [];
}

export async function createLocationItem(baseUrl, payload) {
    const { data } = await axios.post(baseUrl, payload, config);
    return data?.data;
}

export async function updateLocationItem(baseUrl, id, payload) {
    const { data } = await axios.put(`${baseUrl}/${id}`, payload, config);
    return data?.data;
}

export async function deleteLocationItem(baseUrl, id) {
    await axios.delete(`${baseUrl}/${id}`, config);
}
