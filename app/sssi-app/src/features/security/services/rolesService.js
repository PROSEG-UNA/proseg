import axios from 'axios';

const BASE_URL = `http://localhost:8081/api/auth/roles`;

const TOKEN = ""

const headers = {
    Authorization: `Bearer ${TOKEN}`,
};

export async function fetchRoles() {
    const { data } = await axios.get(`${BASE_URL}/composite`, { headers });
    return data.data;
}

export async function fetchPermissionsByRole(roleName) {
    const { data } = await axios.get(`${BASE_URL}/${roleName}/composites`, { headers });
    return data.data;
}

export async function fetchAllPrivileges() {
    const {data} = await axios.get(`${BASE_URL}/base`, { headers });
    return data.data;
}

export async function updateRole(roleName, privileges) {
    const { data } = await axios.put(`${BASE_URL}/${roleName}`, { roleName, privileges }, { headers });
    return data;
}

export async function createRole(roleName, privileges) {
    const { data } = await axios.post(BASE_URL, { roleName, privileges }, { headers });
    return data;
}

export async function deleteRole(roleName) {
    const { data } = await axios.delete(`${BASE_URL}/${roleName}`, { headers });
    return data;
}