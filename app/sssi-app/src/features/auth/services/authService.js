import axios from 'axios';

const BASE_URL = '/api/auth';

const config = { withCredentials: true };

export async function register(username, password, email, firstName, lastName) {
    const { data } = await axios.post(`${BASE_URL}/register`, { username, password, email, firstName, lastName }, config);
    return data;
}

export async function login(identifier, password) {
    const { data } = await axios.post(`${BASE_URL}/login`, { identifier, password }, config);
    return data;
}

export async function logout() {
    const { data } = await axios.post(`${BASE_URL}/logout`, {}, config);
    return data;
}

export async function getCurrentUser() {
    const { data } = await axios.get(`${BASE_URL}/me`, config);
    return data;
}