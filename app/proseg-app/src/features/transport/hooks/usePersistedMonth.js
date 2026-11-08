import { useCallback, useState } from 'react';
import dayjs from 'dayjs';

const STORED_MONTH_FORMAT = 'YYYY-MM';
const STORED_MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

function readStoredMonth(storageKey) {
    try {
        const stored = localStorage.getItem(storageKey);
        return STORED_MONTH_PATTERN.test(stored) ? dayjs(`${stored}-01`) : null;
    } catch {
        return null;
    }
}

function writeStoredMonth(storageKey, month) {
    try {
        if (month) {
            localStorage.setItem(storageKey, month.format(STORED_MONTH_FORMAT));
        } else {
            localStorage.removeItem(storageKey);
        }
    } catch {
        return;
    }
}

export function usePersistedMonth(storageKey) {
    const [month, setMonth] = useState(() => readStoredMonth(storageKey));

    const changeMonth = useCallback((nextMonth) => {
        setMonth(nextMonth);
        writeStoredMonth(storageKey, nextMonth);
    }, [storageKey]);

    return [month, changeMonth];
}
