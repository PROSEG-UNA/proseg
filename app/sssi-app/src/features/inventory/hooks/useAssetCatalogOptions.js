import { useCallback, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchCatalogOptions } from '../services/catalogService.js';
import { INVENTORY_ENDPOINTS } from '../services/endpoints.js';
import { queryKeys } from '../../../common/query';

const CATALOG_SOURCES = [
    ['brands', INVENTORY_ENDPOINTS.brands],
    ['types', INVENTORY_ENDPOINTS.types],
    ['models', INVENTORY_ENDPOINTS.models],
    ['campuses', INVENTORY_ENDPOINTS.campuses],
    ['buildings', INVENTORY_ENDPOINTS.buildings],
    ['locations', INVENTORY_ENDPOINTS.locations],
];

const EMPTY_OPTIONS = Object.fromEntries(CATALOG_SOURCES.map(([collection]) => [collection, []]));

const withOption = (current, option) => (
    current.some(existing => existing.id === option.id)
        ? current.map(existing => (existing.id === option.id ? option : existing))
        : [option, ...current]
);

const mergeKeepingLocalOnly = (fetched, local) => {
    if (local.length === 0) return fetched;
    const fetchedIds = new Set(fetched.map(option => option.id));
    return [...fetched, ...local.filter(option => !fetchedIds.has(option.id))];
};

export function useAssetCatalogOptions(open) {
    const [localOptions, setLocalOptions] = useState(EMPTY_OPTIONS);

    const { data, isLoading } = useQuery({
        queryKey: queryKeys.inventory.catalogOptions(),
        queryFn: async () => {
            const results = await Promise.all(CATALOG_SOURCES.map(([, url]) => fetchCatalogOptions(url)));
            return Object.fromEntries(
                CATALOG_SOURCES.map(([collection], index) => [collection, results[index] ?? []])
            );
        },
        enabled: Boolean(open),
    });

    const upsertOption = useCallback((collection, option) => {
        if (!option?.id) return;
        setLocalOptions(current => ({
            ...current,
            [collection]: withOption(current[collection] ?? [], option),
        }));
    }, []);

    const options = useMemo(() => Object.fromEntries(
        CATALOG_SOURCES.map(([collection]) => [
            collection,
            mergeKeepingLocalOnly(data?.[collection] ?? [], localOptions[collection] ?? []),
        ])
    ), [data, localOptions]);

    return {
        options,
        upsertOption,
        loadingOptions: isLoading,
    };
}
