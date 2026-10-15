import axios from 'axios';
import { INVENTORY_ENDPOINTS } from './endpoints';

const config = { withCredentials: true };

export async function fetchAssetDashboard(days) {
    const { data } = await axios.get(`${INVENTORY_ENDPOINTS.assets}/dashboard`, {
        ...config,
        params: { days },
    });
    return data?.data ?? null;
}

export async function fetchDashboardAssets({ category, days, page = 0, size = 10 } = {}) {
    const params = new URLSearchParams();
    params.append('category', category);
    params.append('days', String(days));
    params.append('page', String(page));
    params.append('size', String(size));

    const { data } = await axios.get(`${INVENTORY_ENDPOINTS.assets}/dashboard/assets`, {
        ...config,
        params,
    });

    return data?.data ?? {
        content: [],
        page,
        size,
        totalElements: 0,
        totalPages: 0,
        last: true,
    };
}
