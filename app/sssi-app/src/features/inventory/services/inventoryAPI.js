import axios from 'axios';

const API_BASE_URL = '/api/inventory';

const config = { withCredentials: true };

export const inventoryAPI = {
  getItems: async () => {
    const { data } = await axios.get(`${API_BASE_URL}/items`, config);
    return data;
  },

  getItem: async (id) => {
    const { data } = await axios.get(`${API_BASE_URL}/items/${id}`, config);
    return data;
  },

  createItem: async (item) => {
    const { data } = await axios.post(`${API_BASE_URL}/items`, item, config);
    return data;
  },

  updateItem: async (id, item) => {
    const { data } = await axios.put(`${API_BASE_URL}/items/${id}`, item, config);
    return data;
  },

  deleteItem: async (id) => {
    await axios.delete(`${API_BASE_URL}/items/${id}`, config);
  },
};
