export function normalizePageItems(response) {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.items)) return response.items;
    if (Array.isArray(response?.content)) return response.content;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.data?.items)) return response.data.items;
    if (Array.isArray(response?.data?.content)) return response.data.content;
    if (Array.isArray(response?.data?.data)) return response.data.data;
    if (Array.isArray(response?.data?.data?.items)) return response.data.data.items;
    if (Array.isArray(response?.data?.data?.content)) return response.data.data.content;

    return [];
}