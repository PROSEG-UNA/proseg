import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { transportConfig } from '../api';

export const TOUR_SPLIT_MODES = {
    DISTINCT: 'DISTINCT_VEHICLES',
    REPEATING: 'SAME_DRIVER_AND_VEHICLE',
};

export async function fetchTourSplitKeywords(mode) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tourSplit}/keywords`, { ...transportConfig, params: { mode } });
    return data?.data ?? [];
}

export async function fetchTourSplitKeywordById(keywordId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tourSplit}/keywords/${keywordId}`, transportConfig);
    return data?.data ?? null;
}

export async function createTourSplitKeyword(payload) {
    const { data } = await axios.post(`${TRANSPORT_ENDPOINTS.tourSplit}/keywords`, payload, transportConfig);
    return data?.data;
}

export async function updateTourSplitKeyword(keywordId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.tourSplit}/keywords/${keywordId}`, payload, transportConfig);
    return data?.data;
}

export async function deleteTourSplitKeyword(keywordId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.tourSplit}/keywords/${keywordId}`, transportConfig);
    return data?.data;
}

export async function fetchTourSplitSettings() {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.tourSplit}/settings`, transportConfig);
    return data?.data ?? { defaultMode: TOUR_SPLIT_MODES.DISTINCT };
}

export async function updateTourSplitSettings(payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.tourSplit}/settings`, payload, transportConfig);
    return data?.data;
}
