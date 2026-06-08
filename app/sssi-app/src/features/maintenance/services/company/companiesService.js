import axios from 'axios';
import { MAINTENANCE_ENDPOINTS } from '../endpoints';
import { fetchPage, maintenanceConfig } from '../api';

export async function fetchCompanies(options = {}) {
    return fetchPage(MAINTENANCE_ENDPOINTS.companies, options);
}

export async function fetchCompanyById(companyId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.companies}/${companyId}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function createCompany(payload) {
    const { data } = await axios.post(MAINTENANCE_ENDPOINTS.companies, payload, maintenanceConfig);
    return data?.data;
}

export async function inviteCompanyUser(payload) {
    const { data } = await axios.post(`${MAINTENANCE_ENDPOINTS.companies}/users`, payload, maintenanceConfig);
    return data?.data;
}

export async function updateCompany(companyId, payload) {
    const { data } = await axios.put(`${MAINTENANCE_ENDPOINTS.companies}/${companyId}`, payload, maintenanceConfig);
    return data?.data;
}

export async function deleteCompany(companyId) {
    const { data } = await axios.delete(`${MAINTENANCE_ENDPOINTS.companies}/${companyId}`, maintenanceConfig);
    return data?.data;
}

export async function fetchCompanyUsers(companyId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.companies}/${companyId}/users`, maintenanceConfig);
    return data?.data ?? [];
}

export async function fetchCompanyTechnicians(companyId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.companies}/${companyId}/technicians`, maintenanceConfig);
    return data?.data ?? [];
}

export async function assignCompanyUser(companyId, keycloakUserId) {
    const { data } = await axios.post(
        `${MAINTENANCE_ENDPOINTS.companies}/${companyId}/users`,
        { keycloakUserId },
        maintenanceConfig
    );
    return data?.data;
}

export async function assignCompanyUsersBulk(companyId, keycloakUserIds = []) {
    const { data } = await axios.post(
        `${MAINTENANCE_ENDPOINTS.companies}/${companyId}/users/batch`,
        { keycloakUserIds },
        maintenanceConfig
    );
    return data?.data ?? [];
}

export async function unassignCompanyUser(companyId, userId) {
    const { data } = await axios.delete(
        `${MAINTENANCE_ENDPOINTS.companies}/${companyId}/users/${userId}`,
        maintenanceConfig
    );
    return data?.data;
}
