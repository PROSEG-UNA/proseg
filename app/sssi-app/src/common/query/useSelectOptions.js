import { useQueries } from '@tanstack/react-query';

const OPTIONS_STALE_TIME = 10 * 60_000;
const EMPTY_OPTIONS = {};

export function useSelectOptions({ fields, fetchOptions, buildQueryKey, enabled = true }) {
    const activeFields = enabled ? (fields ?? []) : [];

    return useQueries({
        queries: activeFields.map((field) => ({
            queryKey: buildQueryKey(field),
            queryFn: () => fetchOptions(field.optionsUrl),
            staleTime: OPTIONS_STALE_TIME,
        })),
        combine: (results) => ({
            options: activeFields.length === 0
                ? EMPTY_OPTIONS
                : Object.fromEntries(activeFields.map((field, index) => [field.key, results[index]?.data ?? []])),
            loading: results.some((result) => result.isLoading),
        }),
    });
}
