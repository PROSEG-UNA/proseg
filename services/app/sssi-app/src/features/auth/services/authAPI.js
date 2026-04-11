export const authAPI = {
  login: async (email, password) => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    return response.json();
  },

  logout: async () => {
    const response = await fetch('/api/auth/logout', {
      method: 'POST',
    });
    return response.json();
  },

  getCurrentUser: async () => {
    const response = await fetch('/api/auth/me');
    return response.json();
  },
};
