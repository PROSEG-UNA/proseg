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

export async function createUser({ username, firstName, lastName, email }) {
    const { data } = await axios.post(
        BASE_URL,
        { username, firstName, lastName, email },
        config,
    );
    return data;
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

export async function assignSingleRoleToUser(userId, roleId) {
    const { data } = await axios.put(`${BASE_URL}/${userId}/roles/${roleId}`, null, config);
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

export async function getInvitationInfo(token) {
    const { data } = await axios.get(`${BASE_URL}/invitation-info`, {
        ...config,
        params: { token },
    });
    return data.data;
}

export async function setPassword(token, password, confirmPassword) {
    const { data } = await axios.post(
        `${BASE_URL}/set-password`,
        { token, password, confirmPassword },
        config,
    );
    return data;
}

export async function resendInvitation(userId) {
    const { data } = await axios.post(
        `${BASE_URL}/${userId}/resend-invitation`,
        null,
        config,
    );

    return data;
}