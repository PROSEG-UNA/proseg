import axios from 'axios';

const BASE_URL = '/api/auth';

export async function register(username, password, email, firstName, lastName) {
    const { data } = await axios.post(`${BASE_URL}/register`, { username, password, email, firstName, lastName });
    return data;
}

export async function login(identifier, password) {
    const { data } = await axios.post(`${BASE_URL}/login`, { identifier, password });
    return data;
}

export async function logout() {
    const { data } = await axios.post(`${BASE_URL}/logout`);
    return data;
}

export async function getCurrentUser(token) {
    const { data } = await axios.get(`${BASE_URL}/me`, {
        headers: { Authorization: `Bearer ${token}` },
    });
    return data;
}