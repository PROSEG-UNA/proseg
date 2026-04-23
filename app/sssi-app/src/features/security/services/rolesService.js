import axios from 'axios';

const BASE_URL = '/api/role';

const config = { withCredentials: true };

export async function fetchRoles() {
    const { data } = await axios.get(`${BASE_URL}/composite`, config);
    return data.data;
}

export async function fetchPermissionsByRole(roleName) {
    const { data } = await axios.get(`${BASE_URL}/${roleName}/composites`, config);
    return data.data;
}

export async function fetchAllPrivileges() {
    const { data } = await axios.get(`${BASE_URL}/base`, config);
    return data.data;
}

export async function fetchUsersByRole(roleName) {
    const { data } = await axios.get(`${BASE_URL}/${roleName}/users`, config);
    return data.data;
}

export async function updateRole(currentName, newName, description, privileges) {
    const { data } = await axios.put(`${BASE_URL}/${currentName}`, { roleName: newName, description, privileges }, config);
    return data;
}

export async function createRole(roleName, description, privileges) {
    const { data } = await axios.post(BASE_URL, { roleName, description, privileges }, config);
    return data;
}

export async function deleteRole(roleName) {
    const { data } = await axios.delete(`${BASE_URL}/${roleName}`, config);
    return data;
}