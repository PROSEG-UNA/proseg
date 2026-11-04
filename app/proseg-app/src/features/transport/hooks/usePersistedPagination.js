import { useEffect, useState } from 'react';

function readPageSize(storageKey, initialPageSize) {
    try {
        const pageSize = JSON.parse(localStorage.getItem(storageKey));
        return Number.isInteger(pageSize) && pageSize > 0 ? pageSize : initialPageSize;
    } catch {
        return initialPageSize;
    }
}

export function usePersistedPagination(storageKey, initialPageSize = 10) {
    const [pagination, setPagination] = useState(() => ({
        pageIndex: 0,
        pageSize: readPageSize(storageKey, initialPageSize),
    }));
    const { pageSize } = pagination;

    useEffect(() => {
        try {
            localStorage.setItem(storageKey, JSON.stringify(pageSize));
        } catch {
            return;
        }
    }, [storageKey, pageSize]);

    return [pagination, setPagination];
}
