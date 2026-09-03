import { useCallback, useState } from 'react';

export function useQueryAlert(queryErrorMessage = null) {
    const [alert, setAlert] = useState(null);
    const [dismissedMessage, setDismissedMessage] = useState(null);

    const visibleAlert = alert ?? (
        queryErrorMessage && queryErrorMessage !== dismissedMessage
            ? { type: 'error', message: queryErrorMessage }
            : null
    );

    const closeAlert = useCallback(() => {
        if (alert) {
            setAlert(null);
            return;
        }
        setDismissedMessage(queryErrorMessage);
    }, [alert, queryErrorMessage]);

    return { alert: visibleAlert, setAlert, closeAlert };
}
