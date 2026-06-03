import axios from 'axios';
import { fetchPage, maintenanceConfig } from './api';

const COMPANIES_BASE = '/api/v1/maintenance/companies';

export async function fetchCompanies(options = {}) {
    return fetchPage(COMPANIES_BASE, options);
}

export async function fetchCompanyById(companyId) {
    const { data } = await axios.get(`${COMPANIES_BASE}/${companyId}`, maintenanceConfig);
    return data?.data ?? null;
}

export async function createCompany(payload) {
    const { data } = await axios.post(COMPANIES_BASE, payload, maintenanceConfig);
    return data?.data;
}

export async function updateCompany(companyId, payload) {
    const { data } = await axios.put(`${COMPANIES_BASE}/${companyId}`, payload, maintenanceConfig);
    return data?.data;
}

export async function deleteCompany(companyId) {
    const { data } = await axios.delete(`${COMPANIES_BASE}/${companyId}`, maintenanceConfig);
    return data?.data;
}

export async function fetchCompanyUsers(companyId) {
    const { data } = await axios.get(`${COMPANIES_BASE}/${companyId}/users`, maintenanceConfig);
    return data?.data ?? [];
}

export async function fetchCompanyTechnicians(companyId) {
    const { data } = await axios.get(`${MAINTENANCE_ENDPOINTS.companies}/${companyId}/technicians`, maintenanceConfig);
    return data?.data ?? [];
}

export async function assignCompanyUser(companyId, keycloakUserId) {
    const { data } = await axios.post(
        `${COMPANIES_BASE}/${companyId}/users`,
        { keycloakUserId },
        maintenanceConfig
    );
    return data?.data;
}

export async function assignCompanyUsersBulk(companyId, keycloakUserIds = []) {
    // backend expects a single DTO { keycloakUserIds: [...] }
    const { data } = await axios.post(
        `${COMPANIES_BASE}/${companyId}/users/batch`,
        { keycloakUserIds },
        maintenanceConfig
    );
    return data?.data ?? [];
}

export async function unassignCompanyUser(companyId, userId) {
    const { data } = await axios.delete(
        `${COMPANIES_BASE}/${companyId}/users/${userId}`,
        maintenanceConfig
    );
    return data?.data;
}

