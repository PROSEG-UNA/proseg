import { useCallback, useEffect, useMemo, useState } from 'react';

function readExpandAll(storageKey) {
    try {
        return JSON.parse(localStorage.getItem(storageKey)) === true;
    } catch {
        return false;
    }
}

export function usePersistedExpansion(storageKey) {
    const [expandAll, setExpandAll] = useState(() => readExpandAll(storageKey));
    const [expanded, setExpanded] = useState(() => expandAll ? true : {});

    useEffect(() => {
        try {
            localStorage.setItem(storageKey, JSON.stringify(expandAll));
        } catch {
            return;
        }
    }, [storageKey, expandAll]);

    const resetExpanded = useCallback(() => {
        setExpanded(expandAll ? true : {});
    }, [expandAll]);

    const tableOptions = useMemo(() => ({
        autoResetExpanded: false,
        onExpandedChange: setExpanded,
        state: { expanded },
        muiExpandAllButtonProps: ({ table }) => ({
            onClick: () => {
                const nextExpandAll = !table.getIsAllRowsExpanded();
                setExpandAll(nextExpandAll);
                setExpanded(nextExpandAll ? true : {});
            },
        }),
    }), [expanded]);

    return { tableOptions, resetExpanded };
}
