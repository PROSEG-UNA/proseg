import axios from 'axios';

const BASE_URL = `http://localhost:8081/api/auth/roles`;

const TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJmcDNNM0FBSWZkOWdnQXQ2ckZFQXlQRVVqdnZXRlR4RjdhNi1jdVp4cWhBIn0.eyJleHAiOjE3NzYzMjU5MDcsImlhdCI6MTc3NjMyNTYwNywianRpIjoiZDc1MTY2OTUtMzJjZS00NmVjLWExZTMtNmY0N2YzZGZjZWVlIiwiaXNzIjoiaHR0cHM6Ly9hdXRoLmRldmJ5Y2hyaXMuY29tL3JlYWxtcy9zc3NpLXJlYWxtIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjVmODEyYzBjLTIyNmItNGE3ZC04MWU4LWRmMzFhYTMzYzdmNCIsInR5cCI6IkJlYXJlciIsImF6cCI6InNzc2ktYXBwIiwic2lkIjoiZWUxNGM1YjgtNzQ3My00YzU0LThkODAtOTY4Y2VjYWVkYjY1IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1zc3NpLXJlYWxtIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJDaHJpc3RvZmVyIENoYXZlcyIsInByZWZlcnJlZF91c2VybmFtZSI6Im1vbmtldXA3IiwiZ2l2ZW5fbmFtZSI6IkNocmlzdG9mZXIiLCJmYW1pbHlfbmFtZSI6IkNoYXZlcyIsImVtYWlsIjoiY3Jpc3RvZmVyY2hhdmVzQGdtYWlsLmNvbSJ9.Hggq_MQtnFRZh4YF8jR6936ssC8sqOeoTfzqVJPlm4y-dLfNzmgtxpuj97lSSso8P1WtB9OuwYBrvLPc0fI8EDvSwiaqSgtk44KWWdsy4MMcV_7Qd-uGIw9_1olOhcmSWohy1FpT079Z7_xePD3Psv7QW7HkHrI4609taTevWfb1m6PkD7uoCBXNAiCUkO4qgQPcQH1Eh36b2pMmqJa5YE-4n5NmqpYebq7M6UGsygTzf0VzXrxzOl8YgD05ktIdfebrGFHvibaykFNVS7IQgKDsg1fvjV5EBlcAF5GBG19LgWzOpmYxRFpqzK8WtffIbATXA5PLpQX_1Zg1hAJ5YQ"

const headers = {
    Authorization: `Bearer ${TOKEN}`,
};

export async function fetchRoles() {
    const { data } = await axios.get(`${BASE_URL}/composite`, { headers });
    return data.data;
}

export async function fetchPermissionsByRole(roleName) {
    const { data } = await axios.get(`${BASE_URL}/${roleName}/composites`, { headers });
    return data.data;
}

export async function fetchAllPrivileges() {
    const {data} = await axios.get(`${BASE_URL}/base`, { headers });
    return data.data;
}

export async function updateRole(roleName, privileges) {
    const { data } = await axios.put(`${BASE_URL}/${roleName}`, { roleName, privileges }, { headers });
    return data;
}

export async function createRole(roleName, privileges) {
    const { data } = await axios.post(BASE_URL, { roleName, privileges }, { headers });
    return data;
}

export async function deleteRole(roleName) {
    const { data } = await axios.delete(`${BASE_URL}/${roleName}`, { headers });
    return data;
}