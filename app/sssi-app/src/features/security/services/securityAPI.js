import axios from 'axios';

const API_BASE_URL = '/api/security';

const config = { withCredentials: true };

export const securityAPI = {
  getPermissions: async () => {
    const { data } = await axios.get(`${API_BASE_URL}/permissions`, config);
    return data;
  },

  getRoles: async () => {
    const { data } = await axios.get(`${API_BASE_URL}/roles`, config);
    return data;
  },

  assignPermission: async (userId, permission) => {
    const { data } = await axios.post(
      `${API_BASE_URL}/permissions/assign`,
      { userId, permission },
      config
    );
    return data;
  },

  revokePermission: async (userId, permission) => {
    await axios.post(
      `${API_BASE_URL}/permissions/revoke`,
      { userId, permission },
      config
    );
  },
};
