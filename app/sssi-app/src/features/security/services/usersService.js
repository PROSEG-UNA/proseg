import axios from 'axios';

const BASE_URL = '/api/user';
const config = { withCredentials: true };

export async function fetchUsers({ page = 0, size = 10 } = {}) {
    const { data } = await axios.get(BASE_URL, {
        ...config,
        params: { page, size },
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

export async function fetchUserStatuses() {
    const { data } = await axios.get(`${BASE_URL}/statuses`, config);
    return Array.isArray(data?.data) ? data.data : [];
}

export async function updateUserApproval(userId, status) {
    const { data } = await axios.patch(`${BASE_URL}/approval/${userId}`, { status }, config);
    return data;
}

export async function assignRoleToUser(userId, roleId) {
    const { data } = await axios.post(`${BASE_URL}/${userId}/roles/${roleId}`, null, config);
    return data;
}

export async function removeRoleFromUser(userId, roleId) {
    const { data } = await axios.delete(`${BASE_URL}/${userId}/roles/${roleId}`, config);
    return data;
}

export async function fetchRolesByUserId(userId) {
    const { data } = await axios.get(`${BASE_URL}/${userId}/roles`, config);
    return data?.data ?? [];
}
