const BASE_URL = '/api/auth';

const getAuthHeaders = () => {
    const tokenKey = import.meta.env.VITE_AUTH_BEARER_TOKEN_KEY || 'auth_token';
    const token = localStorage.getItem(tokenKey) || '';
    return {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
};

export const authAPI = {
    login: async (identifier, password) => {
        const response = await fetch(`${BASE_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ identifier, password }),
        });
        return response.json();
    },

    register: async ({ username, email, password, firstName, lastName }) => {
        const response = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password, firstName, lastName }),
        });
        return response.json();
    },

    getCurrentUser: async () => {
        const response = await fetch(`${BASE_URL}/me`, {
            headers: getAuthHeaders(),
        });
        return response.json();
    },
};
