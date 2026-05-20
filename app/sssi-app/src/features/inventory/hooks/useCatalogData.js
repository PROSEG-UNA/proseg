import { useState, useEffect } from 'react';
import { fetchCatalogPage } from '../services/catalogService';

export function useCatalogData({ baseUrl, pageIndex, pageSize, search = '', filters = {}, sort = [], refreshKey }) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);

    const filtersKey = JSON.stringify(filters);
    const sortKey = sort.join('|');

    const loadData = () => {
        if (!baseUrl) return;
        let cancelled = false;
        setLoading(true);

        fetchCatalogPage(baseUrl, { page: pageIndex, size: pageSize, search, filters, sort })
            .then((result) => {
                if (!cancelled) {
                    setRows(result.content);
                    setTotalElements(result.totalElements);
                    setError(null);
                }
            })
            .catch((err) => {
                if (!cancelled) setError(err?.response?.data?.message ?? err?.message ?? 'Error al cargar datos');
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => { cancelled = true; };
    };

    useEffect(loadData, [baseUrl, pageIndex, pageSize, search, filtersKey, sortKey, refreshKey]);

    return { rows, loading, error, totalElements };
}
