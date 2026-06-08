import axios from 'axios';
import { INVENTORY_ENDPOINTS } from './endpoints';

const config = { withCredentials: true };

export async function fetchEmailsByBuilding(buildingId) {
    const { data } = await axios.get(
        `${INVENTORY_ENDPOINTS.buildings}/${buildingId}/emails`,
        config
    );
    return data?.data ?? [];
}

export async function fetchEmailsByCampus(campusId) {
    const { data } = await axios.get(
        `${INVENTORY_ENDPOINTS.campuses}/${campusId}/emails`,
        config
    );
    return data?.data ?? [];
}

export async function createBuildingEmail(buildingId, payload) {
    const { data } = await axios.post(
        `${INVENTORY_ENDPOINTS.buildings}/${buildingId}/emails`,
        payload,
        config
    );
    return data?.data;
}

export async function deleteBuildingEmail(emailId) {
    await axios.delete(`${INVENTORY_ENDPOINTS.buildingEmails}/${emailId}`, config);
}