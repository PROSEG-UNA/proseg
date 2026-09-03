import axios from 'axios';

export const transportConfig = { withCredentials: true };

export function buildPageParams({ page = 0, size = 10, search = '', filters = {}, sort = [] } = {}) {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (search && search.trim() !== '') {
        params.append('search', search.trim());
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
