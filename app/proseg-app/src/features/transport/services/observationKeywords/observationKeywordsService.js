import axios from 'axios';
import { TRANSPORT_ENDPOINTS } from '../endpoints';
import { fetchPage, transportConfig } from '../api';

export async function fetchObservationKeywords(options = {}) {
    return fetchPage(TRANSPORT_ENDPOINTS.observationKeywords, options);
}

export async function fetchObservationKeywordById(keywordId) {
    const { data } = await axios.get(`${TRANSPORT_ENDPOINTS.observationKeywords}/${keywordId}`, transportConfig);
    return data?.data ?? null;
}

export async function createObservationKeyword(payload) {
    const { data } = await axios.post(TRANSPORT_ENDPOINTS.observationKeywords, payload, transportConfig);
    return data?.data;
}

export async function updateObservationKeyword(keywordId, payload) {
    const { data } = await axios.put(`${TRANSPORT_ENDPOINTS.observationKeywords}/${keywordId}`, payload, transportConfig);
    return data?.data;
}

export async function deleteObservationKeyword(keywordId) {
    const { data } = await axios.delete(`${TRANSPORT_ENDPOINTS.observationKeywords}/${keywordId}`, transportConfig);
    return data?.data;
}
