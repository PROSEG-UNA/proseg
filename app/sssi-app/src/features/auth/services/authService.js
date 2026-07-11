import axios from 'axios';

const BASE_URL = '/api/auth';

const config = { withCredentials: true };

export async function register({ username, password, email, firstName, lastName, captchaToken }) {
    const { data } = await axios.post(
        `${BASE_URL}/register`,
        { username, password, email, firstName, lastName, captchaToken },
        config,
    );
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

export async function forgotPassword(email) {
    const { data } = await axios.post(`${BASE_URL}/forgot-password`, { email }, config);
    return data;
}

export async function resetPassword(token, newPassword) {
    const { data } = await axios.post(`${BASE_URL}/reset-password`, { token, newPassword }, config);
    return data;
}

export async function refreshAccessToken() {
    const { data } = await axios.post(`${BASE_URL}/refresh`, {}, config);
    return data;
}

export async function updateCurrentUserProfileImage(objectName) {
    const { data } = await axios.patch('/api/user/me/profile-image', { objectName }, config);
    return data;
}