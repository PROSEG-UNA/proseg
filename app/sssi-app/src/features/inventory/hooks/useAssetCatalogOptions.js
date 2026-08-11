import { useState, useEffect, useCallback } from 'react';
import { fetchCatalogOptions } from '../services/catalogService.js';
import { INVENTORY_ENDPOINTS } from '../services/endpoints.js';

const CATALOG_SOURCES = [
    ['brands', INVENTORY_ENDPOINTS.brands],
    ['types', INVENTORY_ENDPOINTS.types],
    ['models', INVENTORY_ENDPOINTS.models],
    ['campuses', INVENTORY_ENDPOINTS.campuses],
    ['buildings', INVENTORY_ENDPOINTS.buildings],
    ['locations', INVENTORY_ENDPOINTS.locations],
];

const EMPTY_OPTIONS = Object.fromEntries(CATALOG_SOURCES.map(([collection]) => [collection, []]));

const mergeKeepingLocalOnly = (fetched, current) => {
    const fetchedIds = new Set(fetched.map(option => option.id));
    return [...fetched, ...current.filter(option => !fetchedIds.has(option.id))];
};

const withOption = (current, option) => (
    current.some(existing => existing.id === option.id)
        ? current.map(existing => (existing.id === option.id ? option : existing))
        : [option, ...current]
);

export function useAssetCatalogOptions(open) {
    const [options, setOptions] = useState(EMPTY_OPTIONS);
    const [loadingOptions, setLoadingOptions] = useState(false);

    const upsertOption = useCallback((collection, option) => {
        if (!option?.id) return;
        setOptions(prev => ({ ...prev, [collection]: withOption(prev[collection], option) }));
    }, []);

    const loadCatalogOptions = useCallback(() => {
        if (!open) return undefined;

        let cancelled = false;

        const requestAllOptions = () => {
            setLoadingOptions(true);

            Promise.all(CATALOG_SOURCES.map(([, url]) => fetchCatalogOptions(url)))
                .then(results => {
                    if (cancelled) return;
                    setOptions(prev => {
                        const next = { ...prev };
                        CATALOG_SOURCES.forEach(([collection], index) => {
                            next[collection] = mergeKeepingLocalOnly(results[index], prev[collection]);
                        });
                        return next;
                    });
                })
                .catch(() => {})
                .finally(() => {
                    if (!cancelled) setLoadingOptions(false);
                });
        };

        const loadHandle = window.setTimeout(requestAllOptions, 500);

        return () => {
            cancelled = true;
            window.clearTimeout(loadHandle);
        };
    }, [open]);

    useEffect(loadCatalogOptions, [loadCatalogOptions]);

    return {
        options,
        upsertOption,
        loadingOptions,
    };
}
