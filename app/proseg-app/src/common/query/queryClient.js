import { QueryClient } from '@tanstack/react-query';

const MAX_QUERY_RETRIES = 2;
const DEFAULT_STALE_TIME_MS = 30_000;
const DEFAULT_GC_TIME_MS = 5 * 60_000;

function isClientError(error) {
    const status = error?.response?.status;
    return typeof status === 'number' && status >= 400 && status < 500;
}

function shouldRetryQuery(failureCount, error) {
    if (isClientError(error)) return false;
    return failureCount < MAX_QUERY_RETRIES;
}

export function createQueryClient() {
    return new QueryClient({
        defaultOptions: {
            queries: {
                staleTime: DEFAULT_STALE_TIME_MS,
                gcTime: DEFAULT_GC_TIME_MS,
                retry: shouldRetryQuery,
                refetchOnWindowFocus: false,
            },
            mutations: {
                retry: false,
            },
        },
    });
}

export const queryClient = createQueryClient();
