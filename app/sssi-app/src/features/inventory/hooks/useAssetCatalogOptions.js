import { useState, useEffect } from 'react';
import { fetchCatalogOptions } from '../services/catalogService.js';
import { INVENTORY_ENDPOINTS } from '../services/endpoints.js';

export function useAssetCatalogOptions(open) {
    const [brands, setBrands] = useState([]);
    const [types, setTypes] = useState([]);
    const [models, setModels] = useState([]);
    const [campuses, setCampuses] = useState([]);
    const [buildings, setBuildings] = useState([]);
    const [locations, setLocations] = useState([]);
    const [loadingOptions, setLoadingOptions] = useState(false);

    useEffect(() => {
        if (!open) return undefined;

        let cancelled = false;

        const loadHandle = window.setTimeout(() => {
            setLoadingOptions(true);

            Promise.all([
                fetchCatalogOptions(INVENTORY_ENDPOINTS.brands),
                fetchCatalogOptions(INVENTORY_ENDPOINTS.types),
                fetchCatalogOptions(INVENTORY_ENDPOINTS.models),
                fetchCatalogOptions(INVENTORY_ENDPOINTS.campuses),
                fetchCatalogOptions(INVENTORY_ENDPOINTS.buildings),
                fetchCatalogOptions(INVENTORY_ENDPOINTS.locations),
            ]).then(([b, t, m, s, bd, l]) => {
                if (cancelled) return;
                setBrands(prev => {
                    const ids = new Set(b.map(x => x.id));
                    return [...b, ...prev.filter(x => !ids.has(x.id))];
                });
                setTypes(prev => {
                    const ids = new Set(t.map(x => x.id));
                    return [...t, ...prev.filter(x => !ids.has(x.id))];
                });
                setModels(prev => {
                    const ids = new Set(m.map(x => x.id));
                    return [...m, ...prev.filter(x => !ids.has(x.id))];
                });
                setCampuses(prev => {
                    const ids = new Set(s.map(x => x.id));
                    return [...s, ...prev.filter(x => !ids.has(x.id))];
                });
                setBuildings(prev => {
                    const ids = new Set(bd.map(x => x.id));
                    return [...bd, ...prev.filter(x => !ids.has(x.id))];
                });
                setLocations(prev => {
                    const ids = new Set(l.map(x => x.id));
                    return [...l, ...prev.filter(x => !ids.has(x.id))];
                });
                setLoadingOptions(false);
            }).catch(() => {
                if (!cancelled) {
                    setLoadingOptions(false);
                }
            });
        }, 500);

        return () => {
            cancelled = true;
            window.clearTimeout(loadHandle);
        };
    }, [open]);

    return {
        brands,
        setBrands,
        types,
        setTypes,
        models,
        setModels,
        campuses,
        setCampuses,
        buildings,
        setBuildings,
        locations,
        setLocations,
        loadingOptions,
    };
}
