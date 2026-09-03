export function keepPreviousPage(queryKey) {
    const scope = JSON.stringify(queryKey.slice(0, -1));

    return (previousData, previousQuery) => {
        if (previousData === undefined || !previousQuery) return undefined;
        return JSON.stringify(previousQuery.queryKey.slice(0, -1)) === scope ? previousData : undefined;
    };
}
