import { useQuery } from '@tanstack/react-query';
import {
    fetchTourProcessingDraft,
    fetchTourProcessingDraftDriverSchedule,
    fetchTourProcessingDraftDriverSummaries,
    fetchTourProcessingDraftRow,
    fetchTourProcessingDraftRows,
    fetchTourProcessingDraftSchedule,
    fetchTourProcessingDrafts,
} from '../services/tourProcessing/tourProcessingService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapDraftToRow(draft) {
    return {
        id: draft.id,
        fileName: draft.fileName ?? '—',
        year: draft.year ?? null,
        month: draft.month ?? null,
        status: draft.status ?? '',
        totalRows: draft.totalRows ?? 0,
        duplicateRows: draft.duplicateRows ?? 0,
        conflictingRows: draft.conflictingRows ?? 0,
    };
}

function mapDraftRowToRow(row) {
    return {
        ...row,
        departureTime: row.departureTime ? String(row.departureTime).slice(0, 5) : null,
        returnTime: row.returnTime ? String(row.returnTime).slice(0, 5) : null,
    };
}

export function useTourProcessingDraftsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };
    const listQueryKey = queryKeys.transport.tourProcessingDraftList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchTourProcessingDrafts(requestParams);
            return {
                rows: (response.content ?? []).map(mapDraftToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar los borradores') : null,
        totalElements: data?.totalElements ?? 0,
    };
}

export function useTourProcessingDraft(draftId) {
    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: queryKeys.transport.tourProcessingDraftDetail(draftId),
        queryFn: () => fetchTourProcessingDraft(draftId),
        enabled: Boolean(draftId),
    });

    return {
        draft: data ?? null,
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudo cargar el borrador') : null,
    };
}

export function useTourProcessingDraftRows(draftId, { pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [], rule = '' } = {}, { enabled = true } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort, rule };
    const listQueryKey = queryKeys.transport.tourProcessingDraftRows(draftId, requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        enabled: Boolean(draftId) && enabled,
        queryFn: async () => {
            const response = await fetchTourProcessingDraftRows(draftId, requestParams);
            return {
                rows: (response.content ?? []).map(mapDraftRowToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar las filas del borrador') : null,
        totalElements: data?.totalElements ?? 0,
    };
}

export async function loadTourProcessingDraftRow(draftId, rowId) {
    const row = await fetchTourProcessingDraftRow(draftId, rowId);
    return row ? mapDraftRowToRow(row) : null;
}

export function replaceCachedDraftRow(queryClient, draftId, savedRow) {
    if (!savedRow?.id) return;
    const row = mapDraftRowToRow(savedRow);
    queryClient.setQueriesData({ queryKey: queryKeys.transport.tourProcessingDraftRowLists(draftId) }, (cached) => {
        if (!cached?.rows?.some((current) => current.id === row.id)) return cached;
        return { ...cached, rows: cached.rows.map((current) => (current.id === row.id ? row : current)) };
    });
}

export function invalidateTourProcessingDraft(queryClient, draftId) {
    return Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDraft(draftId) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDraftLists() }),
        queryClient.invalidateQueries({ queryKey: queryKeys.transport.driverMonthLists() }),
    ]);
}

export function useTourProcessingDraftDriverSummaries(draftId) {
    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: queryKeys.transport.tourProcessingDraftDriverSummaries(draftId),
        queryFn: () => fetchTourProcessingDraftDriverSummaries(draftId),
        enabled: Boolean(draftId),
    });

    return {
        surplusCapPerDriver: data?.surplusCapPerDriver ?? 0,
        drivers: data?.drivers ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudo cargar el resumen de choferes') : null,
    };
}

export function useTourProcessingDraftSchedule(draftId, date) {
    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: queryKeys.transport.tourProcessingDraftSchedule(draftId, date),
        queryFn: () => fetchTourProcessingDraftSchedule(draftId, date),
        enabled: Boolean(draftId),
    });

    return {
        drivers: data?.drivers ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudo cargar el control de giras') : null,
    };
}

export function useTourProcessingDraftDriverSchedule(draftId, driverId, { pageIndex = 0, pageSize = 10 } = {}) {
    const requestParams = { page: pageIndex, size: pageSize };
    const listQueryKey = queryKeys.transport.tourProcessingDraftDriverSchedule(draftId, driverId, requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        enabled: Boolean(draftId) && Boolean(driverId),
        queryFn: async () => {
            const response = await fetchTourProcessingDraftDriverSchedule(draftId, driverId, requestParams);
            return {
                tours: response.content ?? [],
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    return {
        tours: data?.tours ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar las giras del chofer') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
