import { useEffect, useState } from 'react';

function readStoredVisibility(storageKey, defaultVisibility) {
    try {
        const parsed = JSON.parse(localStorage.getItem(storageKey));
        return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : defaultVisibility;
    } catch {
        return defaultVisibility;
    }
}

export function usePersistedColumnVisibility(storageKey, defaultVisibility = {}) {
    const [columnVisibility, setColumnVisibility] = useState(() => readStoredVisibility(storageKey, defaultVisibility));

    useEffect(() => {
        try {
            localStorage.setItem(storageKey, JSON.stringify(columnVisibility));
        } catch {
            return;
        }
    }, [storageKey, columnVisibility]);

    return [columnVisibility, setColumnVisibility];
}
