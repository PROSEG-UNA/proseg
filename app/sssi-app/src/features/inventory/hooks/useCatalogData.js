import { useState, useEffect } from 'react';
import { fetchCatalogPage } from '../services/catalogService';

export function useCatalogData({ baseUrl, pageIndex, pageSize, refreshKey }) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);

    const loadData = () => {
        if (!baseUrl) return;
        let cancelled = false;
        setLoading(true);

        fetchCatalogPage(baseUrl, { page: pageIndex, size: pageSize })
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

    useEffect(loadData, [baseUrl, pageIndex, pageSize, refreshKey]);

    return { rows, loading, error, totalElements };
}
