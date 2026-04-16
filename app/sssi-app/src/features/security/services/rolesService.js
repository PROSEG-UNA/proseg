import axios from 'axios';

const BASE_URL = 'http://localhost:8081/api/auth/roles';

const TOKEN = 'eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ0MmhsYkZQWVpJV0ZlbWtZbTloMzlZTjZLTnNNMWhNOFRDVUNTdUI0STlNIn0.eyJleHAiOjE3NzYzMTQ3MDMsImlhdCI6MTc3NjMxNDQwMywianRpIjoib25ydHJvOmUxMGVkMDJlLTU0YmUtYTUwNS01OGQ3LTVhYWJiMWIzYjk4NiIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvc3NzaS1yZWFsbSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJjZjFhZTEyMC1iZTdiLTRjYmEtODYwZi0wYTFhMTA1NjUxMzYiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJzc3NpLWFwcCIsInNpZCI6ImlYX2RndmVWSC1pNnkyRkZIS05TSWxTbSIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cDovL2xvY2FsaG9zdDo1MTczIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJkZWZhdWx0LXJvbGVzLXNzc2ktcmVhbG0iLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IkZlbGlwZSBHb21leiIsInByZWZlcnJlZF91c2VybmFtZSI6ImZlbGlwZSIsImdpdmVuX25hbWUiOiJGZWxpcGUiLCJmYW1pbHlfbmFtZSI6IkdvbWV6IiwiZW1haWwiOiJmZWxpcGVAc3NzaS5jb20ifQ.eTH5_PpESDXiBTwfVeefO5N1v8g8fHBPKGj_opEt9db94gha8DZZd1ztcy_0_AFLliimow2RG47iDa-rTFXxSP_yXUer-69BHrUf0DspTVCi0VYMOqvllhV98_X88QHzFacPckpnaC1-1EVi_wuX4BhSYrkGJPHg8EJ4bkUSRuDRXqgiGjc8YcwIRNJWID4vAqOVg52TZI1qv36-B0_boNgR5B2yaBIdJ3OT-rBJo9AVxCy22ZBG4rjGjXwV9Zr9yHNw8tCe3JhRM-H7UBatYmXMNXfbsGGQk8n4XyUx_Hj7fTwJhMZDU1Vp1aGBOJjgwNRET7fc6gZ5guDG1J2Kxg';

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