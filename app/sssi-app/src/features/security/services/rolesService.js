import axios from 'axios';

const BASE_URL = `/api/auth/roles`;

const getAuthHeaders = () => {
    const tokenKey = import.meta.env.VITE_AUTH_BEARER_TOKEN_KEY || 'auth_token';
    const token = localStorage.getItem(tokenKey) || '';
    return token ? { Authorization: `Bearer ${token}` } : {};
};

export async function fetchRoles() {
    const { data } = await axios.get(`${BASE_URL}/composite`, { headers: getAuthHeaders() });
    return data.data;
}

export async function fetchPermissionsByRole(roleName) {
    const { data } = await axios.get(`${BASE_URL}/${roleName}/composites`, { headers: getAuthHeaders() });
    return data.data;
}

export async function fetchAllPrivileges() {
    const { data } = await axios.get(`${BASE_URL}/base`, { headers: getAuthHeaders() });
    return data.data;
}

export async function updateRole(roleName, privileges) {
    const { data } = await axios.put(`${BASE_URL}/${roleName}`, { roleName, privileges }, { headers: getAuthHeaders() });
    return data;
}

export async function createRole(roleName, privileges) {
    const { data } = await axios.post(BASE_URL, { roleName, privileges }, { headers: getAuthHeaders() });
    return data;
}

export async function deleteRole(roleName) {
    const { data } = await axios.delete(`${BASE_URL}/${roleName}`, { headers: getAuthHeaders() });
    return data;
}