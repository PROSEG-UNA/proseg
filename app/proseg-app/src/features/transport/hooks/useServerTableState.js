import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDebounce } from '../../../common/hooks/useDebounce.js';

const FILTER_DEBOUNCE_MS = 350;
const DEFAULT_DENSITY = 'compact';
const DENSITIES = ['compact', 'comfortable', 'spacious'];

function resolveUpdater(updater, currentValue) {
    return typeof updater === 'function' ? updater(currentValue) : updater;
}

function firstPage(currentValue) {
    return currentValue.pageIndex === 0 ? currentValue : { ...currentValue, pageIndex: 0 };
}

function isPlainObject(value) {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function readStoredState(storageKey) {
    if (!storageKey) return {};
    try {
        const parsed = JSON.parse(localStorage.getItem(storageKey));
        return isPlainObject(parsed) ? parsed : {};
    } catch {
        return {};
    }
}

export function useServerTableState({ initialSorting, filterableColumns, initialPageSize = 10, storageKey }) {
    const [stored] = useState(() => readStoredState(storageKey));

    const [pagination, setPagination] = useState({
        pageIndex: 0,
        pageSize: Number.isInteger(stored.pageSize) && stored.pageSize > 0 ? stored.pageSize : initialPageSize,
    });
    const [globalFilter, setGlobalFilter] = useState(typeof stored.globalFilter === 'string' ? stored.globalFilter : '');
    const [columnFilters, setColumnFilters] = useState(Array.isArray(stored.columnFilters) ? stored.columnFilters : []);
    const [sorting, setSorting] = useState(Array.isArray(stored.sorting) ? stored.sorting : initialSorting);
    const [columnVisibility, setColumnVisibility] = useState(isPlainObject(stored.columnVisibility) ? stored.columnVisibility : {});
    const [density, setDensity] = useState(DENSITIES.includes(stored.density) ? stored.density : DEFAULT_DENSITY);
    const [showColumnFilters, setShowColumnFilters] = useState(stored.showColumnFilters === true);

    const pageSize = pagination.pageSize;

    useEffect(() => {
        if (!storageKey) return;
        localStorage.setItem(storageKey, JSON.stringify({
            pageSize,
            globalFilter,
            columnFilters,
            sorting,
            columnVisibility,
            density,
            showColumnFilters,
        }));
    }, [storageKey, pageSize, globalFilter, columnFilters, sorting, columnVisibility, density, showColumnFilters]);

    const debouncedGlobalFilter = useDebounce(globalFilter, FILTER_DEBOUNCE_MS);
    const debouncedColumnFilters = useDebounce(columnFilters, FILTER_DEBOUNCE_MS);

    const filters = useMemo(() => {
        const result = {};
        debouncedColumnFilters.forEach(({ id, value }) => {
            if (!filterableColumns.has(id)) return;
            if (value === null || value === undefined || value === '') return;
            result[id] = value;
        });
        return result;
    }, [debouncedColumnFilters, filterableColumns]);

    const sort = useMemo(
        () => sorting.map(({ id, desc }) => `${id},${desc ? 'desc' : 'asc'}`),
        [sorting],
    );

    const handleGlobalFilterChange = useCallback((updater) => {
        setGlobalFilter((currentValue) => resolveUpdater(updater, currentValue));
        setPagination(firstPage);
    }, []);

    const handleColumnFiltersChange = useCallback((updater) => {
        setColumnFilters((currentValue) => resolveUpdater(updater, currentValue));
        setPagination(firstPage);
    }, []);

    const handleSortingChange = useCallback((updater) => {
        setSorting((currentValue) => resolveUpdater(updater, currentValue));
        setPagination(firstPage);
    }, []);

    const tableOptions = useMemo(() => ({
        manualPagination: true,
        manualFiltering: true,
        manualSorting: true,
        onPaginationChange: setPagination,
        onGlobalFilterChange: handleGlobalFilterChange,
        onColumnFiltersChange: handleColumnFiltersChange,
        onSortingChange: handleSortingChange,
        onColumnVisibilityChange: setColumnVisibility,
        onDensityChange: setDensity,
        onShowColumnFiltersChange: setShowColumnFilters,
        state: {
            pagination,
            globalFilter,
            columnFilters,
            sorting,
            columnVisibility,
            density,
            showColumnFilters,
        },
    }), [
        pagination,
        globalFilter,
        columnFilters,
        sorting,
        columnVisibility,
        density,
        showColumnFilters,
        handleGlobalFilterChange,
        handleColumnFiltersChange,
        handleSortingChange,
    ]);

    return {
        requestParams: {
            pageIndex: pagination.pageIndex,
            pageSize: pagination.pageSize,
            search: debouncedGlobalFilter,
            filters,
            sort,
        },
        tableOptions,
    };
}
