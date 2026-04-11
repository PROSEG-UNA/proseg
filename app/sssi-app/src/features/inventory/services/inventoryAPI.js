export const inventoryAPI = {
  getItems: async () => {
    const response = await fetch('/api/inventory/items');
    return response.json();
  },

  getItem: async (id) => {
    const response = await fetch(`/api/inventory/items/${id}`);
    return response.json();
  },

  createItem: async (item) => {
    const response = await fetch('/api/inventory/items', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(item),
    });
    return response.json();
  },

  updateItem: async (id, item) => {
    const response = await fetch(`/api/inventory/items/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(item),
    });
    return response.json();
  },

  deleteItem: async (id) => {
    await fetch(`/api/inventory/items/${id}`, {
      method: 'DELETE',
    });
  },
};
