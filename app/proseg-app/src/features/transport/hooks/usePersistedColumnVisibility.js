import { useEffect, useState } from 'react';

function readStoredVisibility(storageKey) {
    try {
        const parsed = JSON.parse(localStorage.getItem(storageKey));
        return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
    } catch {
        return {};
    }
}

export function usePersistedColumnVisibility(storageKey) {
    const [columnVisibility, setColumnVisibility] = useState(() => readStoredVisibility(storageKey));

    useEffect(() => {
        try {
            localStorage.setItem(storageKey, JSON.stringify(columnVisibility));
        } catch {
            return;
        }
    }, [storageKey, columnVisibility]);

    return [columnVisibility, setColumnVisibility];
}
