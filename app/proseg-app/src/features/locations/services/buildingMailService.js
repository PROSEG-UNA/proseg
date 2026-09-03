import axios from 'axios';
import { LOCATION_ENDPOINTS } from './endpoints';

const config = { withCredentials: true };

export async function fetchEmailsByBuilding(buildingId) {
    const { data } = await axios.get(
        `${LOCATION_ENDPOINTS.buildings}/${buildingId}/emails`,
        config
    );
    return data?.data ?? [];
}

export async function fetchEmailsByCampus(campusId) {
    const { data } = await axios.get(
        `${LOCATION_ENDPOINTS.campuses}/${campusId}/emails`,
        config
    );
    return data?.data ?? [];
}

export async function createBuildingEmail(buildingId, payload) {
    const { data } = await axios.post(
        `${LOCATION_ENDPOINTS.buildings}/${buildingId}/emails`,
        payload,
        config
    );
    return data?.data;
}

export async function deleteBuildingEmail(emailId) {
    await axios.delete(`${LOCATION_ENDPOINTS.buildingEmails}/${emailId}`, config);
}
