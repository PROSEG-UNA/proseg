import axios from 'axios';

const BASE_URL = '/api/v1/inventory/assets';
const config = { withCredentials: true };

export async function fetchAssetArchives(assetId) {
  const { data } = await axios.get(`${BASE_URL}/${assetId}/asset-archives`, config);
  return Array.isArray(data?.data) ? data.data : [];
}
