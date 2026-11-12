import { useCallback, useState } from 'react';

function readStoredOption(storageKey, allowedOptions, fallback) {
    try {
        const stored = localStorage.getItem(storageKey);
        return allowedOptions.includes(stored) ? stored : fallback;
    } catch {
        return fallback;
    }
}

export function usePersistedOption(storageKey, allowedOptions, fallback) {
    const [option, setOption] = useState(() => readStoredOption(storageKey, allowedOptions, fallback));

    const changeOption = useCallback((nextOption) => {
        setOption(nextOption);
        try {
            localStorage.setItem(storageKey, nextOption);
        } catch {
            return;
        }
    }, [storageKey]);

    return [option, changeOption];
}
