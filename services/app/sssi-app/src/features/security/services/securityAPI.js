export const securityAPI = {
  getPermissions: async () => {
    const response = await fetch('/api/security/permissions');
    return response.json();
  },

  getRoles: async () => {
    const response = await fetch('/api/security/roles');
    return response.json();
  },

  assignPermission: async (userId, permission) => {
    const response = await fetch('/api/security/permissions/assign', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, permission }),
    });
    return response.json();
  },

  revokePermission: async (userId, permission) => {
    await fetch('/api/security/permissions/revoke', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, permission }),
    });
  },
};
