import axios from 'axios';

const config = { withCredentials: true };

export async function fetchCatalogPage(baseUrl, { page = 0, size = 10 } = {}) {
    const { data } = await axios.get(baseUrl, {
        ...config,
        params: { page, size },
    });
    return data?.data ?? { content: [], page, size, totalElements: 0, totalPages: 0, last: true };
}

export async function fetchCatalogOptions(baseUrl) {
    const { data } = await axios.get(baseUrl, {
        ...config,
        params: { page: 0, size: 200 },
    });
    return data?.data?.content ?? [];
}

export async function createCatalogItem(baseUrl, payload) {
    const { data } = await axios.post(baseUrl, payload, config);
    return data?.data;
}

export async function updateCatalogItem(baseUrl, id, payload) {
    const { data } = await axios.put(`${baseUrl}/${id}`, payload, config);
    return data?.data;
}

export async function deleteCatalogItem(baseUrl, id) {
    await axios.delete(`${baseUrl}/${id}`, config);
}
